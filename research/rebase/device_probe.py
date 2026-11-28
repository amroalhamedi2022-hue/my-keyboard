"""Real touch evaluation of the mature IME against the existing synthetic editor fixture.
Requires debug-only geometry export. No input text injection and no personal data.
"""
import argparse
import json
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path
import re

PACKAGE="com.zerokeyboard.ime.debug"
IME=PACKAGE+"/com.zerokeyboard.ime.ZeroImeService"
FIXTURE="com.zerokeyboard.ime/.ImeProbeActivity"
MICROPHONE = -233
DISCARD = -10001
RECORDING = -10002
SETTINGS = PACKAGE + "/helium314.keyboard.settings.SettingsActivity"

def adb(*args):
    return subprocess.run(["adb","-s","emulator-5554",*args],check=True,capture_output=True,timeout=45).stdout

def wait(fn, seconds=35):
    end=time.monotonic()+seconds
    last=None
    while time.monotonic()<end:
        try:
            value=fn()
            if value is not None and value is not False:return value
        except (subprocess.SubprocessError,ValueError,ET.ParseError) as e:last=str(e)
        time.sleep(.3)
    raise AssertionError("condition not observed: "+str(last))

def tree():
    adb("shell","uiautomator","dump","/data/local/tmp/zero-mature.xml")
    return ET.fromstring(adb("shell","cat","/data/local/tmp/zero-mature.xml"))

def field(name):
    return next((n for n in tree().iter("node") if n.get("content-desc")==name),None)

def focus(name):
    def find():
        n=field(name)
        if n is None:return None
        bounds=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",n.get("bounds",""))
        return tuple(map(int,bounds.groups())) if bounds else None
    x1,y1,x2,y2=wait(find)
    adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
    wait(lambda:(lambda n:n is not None and n.get("focused")=="true")(field(name)))

def geometry():
    return json.loads(adb("shell","run-as",PACKAGE,"cat","files/zero-eval-geometry.json"))

def key(code):
    return next((v for v in geometry() if v["code"]==code or (97<=code<=122 and v["code"]==code-32)),None)

def tap(code):
    k=wait(lambda:key(code))
    adb("shell","input","tap",str(k["x"]),str(k["y"]))
    time.sleep(.35)

def check_text(name,expected):
    wait(lambda:(lambda n:n is not None and n.get("text","").lower()==expected)(field(name)))
    print("PASS editor text",repr(expected),flush=True)

def language(ar):
    expected=ord("ض") if ar else ord("q")
    alternative=ord("q") if ar else ord("ض")
    def found():return key(expected) or key(alternative)
    wait(found)
    if not key(expected):
        if not key(-227):raise AssertionError("required language switch key missing; codes="+str([v["code"] for v in geometry()]))
        tap(-227)
    wait(lambda:key(expected))

def snapshot(out,name):
    (out/(name+".png")).write_bytes(adb("exec-out","screencap","-p"))

def find_node(predicate):
    return next((node for node in tree().iter("node") if predicate(node)), None)

def touch_node(node):
    left, top, right, bottom = bounds(node)
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))

def bounds(node):
    coords = tuple(map(int, re.findall(r"\d+", node.get("bounds", ""))))
    assert len(coords) == 4, node.attrib
    return coords

def named(text):
    return find_node(lambda node: text in (node.get("text"), node.get("content-desc")))

def adaptive_settings_smoke(out, checks):
    adb("shell", "am", "start", "-W", "-n", SETTINGS)
    wait(lambda: named("Zero Voice & Glide"))
    assert named("Try the keyboard") is None, "phone layout unexpectedly shows tablet preview"
    snapshot(out, "settings-phone")
    try:
        adb("shell", "wm", "size", "1600x2560")
        adb("shell", "wm", "density", "240")
        preview = wait(lambda: named("Try the keyboard"))
        sidebar = wait(lambda: named("Appearance"))
        assert bounds(sidebar)[2] < bounds(preview)[0], "preview is not alongside the category list"
        snapshot(out, "settings-tablet-preview")
        touch_node(sidebar)
        wait(lambda: named("Colors (night)"))
        assert named("Zero Voice & Glide") is not None, "category list disappeared"
        snapshot(out, "settings-tablet-appearance")
        checks.append("phone settings and tablet category/detail panes exposed on Android 13")
    finally:
        adb("shell", "wm", "density", "reset")
        adb("shell", "wm", "size", "reset")

def permission_button(name):
    return find_node(lambda n: n.get("resource-id", "").endswith(":id/" + name))

def voice_smoke(out, checks):
    tap(MICROPHONE)
    deny = wait(lambda: permission_button("permission_deny_button"))
    snapshot(out, "voice-permission")
    touch_node(deny)
    check_text("zero_probe_normal", "qwضa")
    checks.append("microphone requests system permission and denial returns to the editor")

    tap(MICROPHONE)
    touch_node(wait(lambda: permission_button("permission_allow_foreground_only_button")))
    check_text("zero_probe_normal", "qwضa")
    assert key(RECORDING) is None
    tap(MICROPHONE)
    wait(lambda: key(RECORDING), seconds=90)
    discard = wait(lambda: key(DISCARD))
    assert discard["y"] >= wait(lambda: key(ord("q")))["y"], "voice controls escaped the keyboard"
    snapshot(out, "voice-recording")
    tap(DISCARD)
    wait(lambda: key(DISCARD) is None)
    check_text("zero_probe_normal", "qwضa")
    checks.append("bundled model prepares, microphone capture starts on explicit tap and Discard preserves text")

def transcription_smoke(out, checks, wav_path, port):
    from emulator_audio import replay_wav

    tap(MICROPHONE)
    wait(lambda: key(RECORDING), seconds=90)
    started = time.monotonic()
    replay_wav(wav_path, port)
    expected = "what your country can do for you"

    def transcript():
        node = field("zero_probe_normal")
        text = "" if node is None else node.get("text", "").casefold()
        return text if expected in " ".join(re.findall(r"[a-z]+", text)) else None

    inserted = wait(transcript, seconds=90)
    (out / "english-dictation.json").write_text(json.dumps({
        "editor_text": inserted,
        "replay_to_observed_insertion_seconds": round(time.monotonic() - started, 3),
        "required_phrase": expected,
    }, indent=2))
    snapshot(out, "voice-inserted")
    checks.append("public English WAV decoded through Android microphone and inserted through the IME")

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--candidate",type=Path,required=True);p.add_argument("--fixture",type=Path,required=True);p.add_argument("--out",type=Path,required=True)
    p.add_argument("--english-wav", type=Path)
    p.add_argument("--adaptive-settings", action="store_true")
    p.add_argument("--emulator-grpc-port", type=int, default=8554)
    a=p.parse_args();a.out.mkdir(parents=True,exist_ok=True);result={"checks":[]}
    try:
        adb("install","-r",str(a.fixture));adb("install","-r",str(a.candidate))
        wait(lambda:IME in adb("shell","ime","list","-a","-s").decode())
        adb("shell","ime","enable",IME);adb("shell","ime","set",IME)
        adb("shell","settings","put","secure","show_ime_with_hard_keyboard","1")
        settings_error = None
        if a.adaptive_settings:
            try:
                adaptive_settings_smoke(a.out, result["checks"])
            except Exception as error:
                settings_error = error
                result["settings_error"] = str(error)
                try:
                    snapshot(a.out, "settings-failure")
                except (OSError, subprocess.SubprocessError):
                    pass
        adb("shell","am","start","-W","-n",FIXTURE)
        snapshot(a.out,"fixture-before-focus")
        focus("zero_probe_normal")
        snapshot(a.out,"normal-initial")
        language(False);snapshot(a.out,"english")
        for c in "qwe":tap(ord(c))
        # Some editors start with auto-capitalization. Explicitly distinguish it from missing input.
        wait(lambda:(lambda n:n is not None and n.get("text","").lower()=="qwe")(field("zero_probe_normal")))
        result["checks"].append("three English real touches"); print("PASS English real touches",flush=True)
        tap(-7)
        wait(lambda:(lambda n:n is not None and n.get("text","").lower()=="qw")(field("zero_probe_normal")))
        result["checks"].append("delete real touch"); print("PASS delete real touch",flush=True)
        language(True);snapshot(a.out,"arabic")
        tap(ord("ض"))
        wait(lambda:(lambda n:n is not None and n.get("text","").lower()=="qwض")(field("zero_probe_normal")))
        result["checks"].append("mixed Arabic/English touches"); print("PASS bilingual real touches",flush=True)
        snapshot(a.out,"mixed-text")
        language(False)
        tap(ord("a"))
        wait(lambda:(lambda n:n is not None and n.get("text","").lower()=="qwضa")(field("zero_probe_normal")))
        result["checks"].append("switch back to English")
        voice_smoke(a.out, result["checks"])
        if a.english_wav:
            transcription_smoke(a.out, result["checks"], a.english_wav, a.emulator_grpc_port)
        focus("zero_probe_password");snapshot(a.out,"password")
        wait(lambda: key(MICROPHONE) is None)
        result["checks"].append("voice toolbar action hidden in password field")
        if settings_error is not None:
            raise settings_error
    except Exception as e:
        result["error"]=str(e)
        if isinstance(e,subprocess.CalledProcessError):result["stderr"]=(e.stderr or b"").decode(errors="replace")[-2000:]
        raise
    finally:
        try:snapshot(a.out,"final")
        except Exception:pass
        try:(a.out/"window.xml").write_bytes(adb("shell","cat","/data/local/tmp/zero-mature.xml"))
        except Exception:pass
        try:(a.out/"keyboard-geometry.json").write_text(json.dumps(geometry()), encoding="utf-8")
        except (OSError, ValueError, subprocess.SubprocessError):pass
        (a.out/"report.json").write_text(json.dumps(result,ensure_ascii=False,indent=2))
if __name__=="__main__":main()
