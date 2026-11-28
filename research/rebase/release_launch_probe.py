"""Install the actual release APK (temporary CI signature), exercise onboarding and upgrade.
No owner secrets, no private input; fail on process death or missing UI."""
import json
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG="com.zerokeyboard.ime"
ACT=PKG+"/helium314.keyboard.settings.SettingsActivity"
IME_CLASS=PKG+".ZeroImeService"
out=Path(sys.argv[3]);out.mkdir(parents=True,exist_ok=True)
expected_version = int(sys.argv[4]) if len(sys.argv) > 4 else 103
report={"checks":[]}

class AdbError(subprocess.SubprocessError):
    pass

def adb(*args):
    result = subprocess.run(["adb", "-s", "emulator-5554", *args], capture_output=True,
                            timeout=50, encoding="utf-8", errors="replace")
    if result.returncode:
        raise AdbError(f"adb {args!r} failed ({result.returncode}): "
                           f"{result.stdout.strip()} {result.stderr.strip()}")
    return result.stdout
def wait(fn,seconds=40):
    until=time.monotonic()+seconds
    last=None
    while time.monotonic()<until:
        try:
            val=fn()
            if val is not None:
                return val
        except (subprocess.SubprocessError,ET.ParseError) as e:last=str(e)
        time.sleep(.5)
    raise AssertionError("UI condition not reached: "+str(last))
def registered_ime():
    for ime in adb("shell", "ime", "list", "-a", "-s").splitlines():
        component = ime.strip()
        package, _, cls = component.partition("/")
        if cls.startswith("."):
            cls = package + cls
        if package == PKG and cls == IME_CLASS:
            return component
    return None


def tree():
    adb("shell","uiautomator","dump","/data/local/tmp/zero-launch.xml")
    return ET.fromstring(adb("shell","cat","/data/local/tmp/zero-launch.xml"))
def node(text):
    return next((n for n in tree().iter("node") if n.get("text")==text or n.get("content-desc")==text),None)
def tap(text):
    n=wait(lambda:node(text))
    coords=list(map(int,re.findall(r"\d+",n.get("bounds",""))))
    assert len(coords)==4
    adb("shell","input","tap",str((coords[0]+coords[2])//2),str((coords[1]+coords[3])//2))
def snap(name):
    (out/(name+".png")).write_bytes(subprocess.check_output(["adb","-s","emulator-5554","exec-out","screencap","-p"],timeout=30))
def launch():
    adb("shell","am","force-stop",PKG)
    result=adb("shell","am","start","-W","-n",ACT)
    assert "Status: ok" in result,result
def alive():
    assert adb("shell","pidof",PKG).strip()
def passed(name):
    alive();report["checks"].append(name);print("PASS "+name,flush=True)
def verify_version():
    package_dump = adb("shell", "dumpsys", "package", PKG)
    assert re.search(rf"\bversionCode={expected_version}\b", package_dump), package_dump


def onboarding(prefix):
    launch()
    wait(lambda:node("Get started"))
    snap(prefix+"-welcome");passed(prefix+" disabled IME welcome")
    tap("Get started")
    wait(lambda:node("Enable in Settings"))
    passed(prefix+" welcome action")
    ime = wait(registered_ime)
    adb("shell","ime","enable",ime)
    launch();wait(lambda:node("Switch input methods"))
    snap(prefix+"-enabled");passed(prefix+" enabled not selected")
    adb("shell","ime","set",ime)
    assert adb("shell","settings","get","secure","default_input_method").strip() == ime
    wait(lambda:node("Configure the keyboard"))
    tap("Configure the keyboard")
    wait(lambda:node("Zero Voice & Glide"))
    passed(prefix+" wizard completed")
    snap(prefix+"-settings");passed(prefix+" selected settings")
    tap("Zero Voice & Glide");wait(lambda:node("Enable offline voice"))
    snap(prefix+"-voice");passed(prefix+" voice settings")
    adb("shell","input","keyevent","4")
    wait(lambda:node("Zero Voice & Glide"))
    adb("shell","input","keyevent","3")
    adb("shell","am","start","-W","-n",ACT)
    wait(lambda:node("Zero Voice & Glide"))
    passed(prefix+" warm settings launch")
try:
    adb("logcat", "-b", "all", "-c")
    # Update from actual previous release, with both APKs re-signed by the same ephemeral CI certificate.
    adb("install",sys.argv[1])
    adb("install","-r",sys.argv[2])
    verify_version()
    onboarding("upgrade")
    adb("uninstall",PKG)
    adb("install",sys.argv[2])
    verify_version()
    onboarding("fresh")
    logs = adb("logcat", "-b", "crash", "-d")
    assert not re.search(r"(?:Process: |Cmdline: |>>> )com\.zerokeyboard\.ime(?:[, :]|$)", logs), logs
    report["checks"].append("no Zero AndroidRuntime crash")
except Exception as exc:
    report["error"]=str(exc)
    try:
        (out/"registered-imes.txt").write_text(adb("shell", "ime", "list", "-a", "-s"))
    except Exception:
        pass
    try:snap("failure")
    except Exception:pass
    try:
        (out/"failure-ui.xml").write_text(ET.tostring(tree(), encoding="unicode"))
    except Exception:
        pass
    raise
finally:
    for filename, args in [
        ("android-runtime.txt", ("logcat", "-d", "-s", "AndroidRuntime:E")),
        ("crash-buffer.txt", ("logcat", "-b", "crash", "-d")),
        ("process-events.txt", ("logcat", "-b", "events", "-d", "-s",
                                "am_proc_died", "am_kill", "am_crash", "am_anr")),
        ("exit-info.txt", ("shell", "dumpsys", "activity", "exit-info", PKG)),
        ("memory.txt", ("shell", "dumpsys", "meminfo", PKG)),
    ]:
        try:
            (out / filename).write_text(adb(*args))
        except subprocess.SubprocessError as error:
            (out / filename).write_text(str(error))
    (out/"report.json").write_text(json.dumps(report,ensure_ascii=False,indent=2))
