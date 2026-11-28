"""ADB debug-only IME smoke runner. Use a disposable Android emulator, never a personal device.
Requires already-built current debug APK and an online emulator. No Gradle invocation.
"""
import argparse
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "com.zerokeyboard.ime"
IME = PACKAGE + "/.ZeroImeService"
ACTIVITY = PACKAGE + "/.ImeProbeActivity"


def center(bounds):
    m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not m:
        raise ValueError("invalid node bounds")
    x1, y1, x2, y2 = map(int, m.groups())
    if x2 <= x1 or y2 <= y1:
        raise ValueError("empty node bounds")
    return (x1+x2)//2, (y1+y2)//2


def matching(xml, description):
    return [n for n in xml.iter("node") if n.get("content-desc") == description]


class Probe:
    def __init__(self, serial, output):
        self.serial = serial
        self.output = output
        self.results = []

    def adb(self, *args):
        return subprocess.run(["adb", "-s", self.serial, *args], check=True,
                              capture_output=True, timeout=45).stdout

    def await_ime_registration(self, seconds=45):
        deadline = time.monotonic() + seconds
        while time.monotonic() < deadline:
            try:
                ids = self.adb("shell", "ime", "list", "-a", "-s").decode(errors="replace").splitlines()
                if any(value.strip() in (IME, PACKAGE + "/" + PACKAGE + ".ZeroImeService") for value in ids):
                    return
            except subprocess.CalledProcessError:
                pass
            time.sleep(1)
        raise RuntimeError("Zero Keyboard IME not registered after install and activity launch")

    def tree(self):
        self.adb("shell", "uiautomator", "dump", "/data/local/tmp/zero_probe.xml")
        return ET.fromstring(self.adb("shell", "cat", "/data/local/tmp/zero_probe.xml"))

    def wait(self, predicate, seconds=20):
        deadline = time.monotonic() + seconds
        while time.monotonic() < deadline:
            try:
                xml = self.tree()
                if predicate(xml):
                    return xml
            except (ET.ParseError, subprocess.CalledProcessError):
                pass
            time.sleep(0.35)
        raise AssertionError("UI predicate not observed")

    def tap(self, description):
        xml = self.wait(lambda x: len(matching(x, description)) == 1)
        node = matching(xml, description)[0]
        x, y = center(node.attrib["bounds"])
        self.adb("shell", "input", "tap", str(x), str(y))

    def check(self, name, predicate):
        try:
            self.wait(predicate)
        except Exception:
            self.results.append({"test": name, "status": "fail"})
            raise
        self.results.append({"test": name, "status": "pass"})
        print("PASS", name, flush=True)

    def focus(self, field):
        self.tap(field)
        self.wait(lambda x: any(n.get("focused") == "true" for n in matching(x, field))
                  and bool(matching(x, "Delete")))

    def run(self, apk):
        if not self.serial.startswith("emulator-"):
            raise ValueError("only disposable emulator serials are accepted")
        if self.adb("get-state").strip() != b"device":
            raise RuntimeError("emulator not online")
        if self.adb("shell", "getprop", "sys.boot_completed").strip() != b"1":
            raise RuntimeError("emulator not fully booted")
        previous = self.adb("shell", "settings", "get", "secure", "default_input_method").decode().strip()
        try:
            self.adb("install", "-r", str(apk))
            self.adb("shell", "am", "start", "-W", "-n", ACTIVITY)
            self.await_ime_registration()
            self.adb("shell", "ime", "enable", IME)
            self.adb("shell", "ime", "set", IME)
            self.adb("shell", "settings", "put", "secure", "show_ime_with_hard_keyboard", "1")
            self.adb("shell", "am", "start", "-W", "-n", ACTIVITY)
            self.focus("zero_probe_normal")
            self.check("normal voice and toolbar available", lambda x: bool(matching(x,"Offline voice typing")) and bool(matching(x,"Copy selected")))
            xml=self.tree()
            if not matching(xml,"q"):
                self.tap("Switch language")
            self.tap("q")
            self.tap("w")
            self.check("English manual text", lambda x: any(n.get("text")=="qw" for n in matching(x,"zero_probe_normal")))
            self.tap("Switch language")
            self.tap("ض")
            self.check("mixed Arabic and English manual text", lambda x: any(n.get("text")=="qwض" for n in matching(x,"zero_probe_normal")))
            self.tap("Emoji panel")
            self.tap("Search emoji")
            xml=self.tree()
            if not matching(xml,"Emoji search c"):
                self.tap("Switch emoji search language")
            for letter in "coffee":
                self.tap("Emoji search "+letter)
            self.check("offline emoji search",lambda x: bool(matching(x,"Insert emoji ☕")))
            self.tap("Insert emoji ☕")
            self.check("emoji inserted in real editor",lambda x: any(n.get("text")=="qwض☕" for n in matching(x,"zero_probe_normal")))
            self.tap("Back to keyboard")
            self.tap("Session clipboard")
            self.tap("Save current clipboard")
            self.check("explicit synthetic clipboard save",lambda x: bool(matching(x,"Paste saved text")))
            self.tap("Paste saved text")
            self.check("saved clipboard text inserted",lambda x: any(n.get("text")=="qwض☕ probe" for n in matching(x,"zero_probe_normal")))
            self.output.with_name("normal-synthetic.png").write_bytes(self.adb("exec-out","screencap","-p"))
            self.focus("zero_probe_password")
            self.check("password hides voice and editor toolbar", lambda x: not matching(x,"Offline voice typing") and not matching(x,"Copy selected") and not matching(x,"Session clipboard") and not matching(x,"Emoji panel") and not any(n.get("content-desc","").startswith("Glide candidate ") for n in x.iter("node")))
            self.focus("zero_probe_numeric")
            self.check("numeric editor shows digit and hides voice", lambda x: bool(matching(x,"1") or matching(x,"١")) and not matching(x,"Offline voice typing"))
            self.focus("zero_probe_private")
            self.check("no-personalized-learning editor hides voice", lambda x: not matching(x,"Offline voice typing"))
        finally:
            try:
                if previous and previous != "null" and previous != IME:
                    self.adb("shell","ime","set",previous)
                self.adb("shell","rm","-f","/data/local/tmp/zero_probe.xml")
            except (subprocess.SubprocessError, OSError):
                # Cleanup must not mask the primary test failure.
                pass

    def report(self, error=None):
        data={"serial": self.serial,"checks":self.results,"error":error,
              "scope":"debug synthetic fields; no microphone, actual glide, provider or performance benchmark"}
        self.output.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial",required=True)
    parser.add_argument("--apk",required=True,type=Path)
    parser.add_argument("--report",required=True,type=Path)
    a=parser.parse_args()
    if not a.apk.is_file():
        parser.error("debug APK missing")
    probe=Probe(a.serial,a.report)
    try:
        probe.run(a.apk.resolve())
    except Exception as exc:
        error = str(exc)
        if isinstance(exc, subprocess.CalledProcessError):
            error += "\nstdout: " + (exc.stdout or b"").decode(errors="replace")[-2000:]
            error += "\nstderr: " + (exc.stderr or b"").decode(errors="replace")[-2000:]
        probe.report(error)
        raise
    probe.report()

if __name__=="__main__":
    main()
