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
IME=PKG+"/com.zerokeyboard.ime.ZeroImeService"
out=Path(sys.argv[3]);out.mkdir(parents=True,exist_ok=True)
report={"checks":[]}
def adb(*args):
    return subprocess.run(["adb","-s","emulator-5554",*args],check=True,capture_output=True,timeout=50).stdout.decode(errors="replace")
def wait(fn,seconds=40):
    until=time.monotonic()+seconds
    last=None
    while time.monotonic()<until:
        try:
            val=fn()
            if val:return val
        except (subprocess.SubprocessError,ET.ParseError) as e:last=str(e)
        time.sleep(.5)
    raise AssertionError("UI condition not reached: "+str(last))
def tree():
    adb("shell","uiautomator","dump","/data/local/tmp/zero-launch.xml")
    return ET.fromstring(adb("shell","cat","/data/local/tmp/zero-launch.xml"))
def node(text):
    return next((n for n in tree().iter("node") if n.get("text")==text or n.get("content-desc")==text),None)
def exists(text):
    return node(text) is not None
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
def onboarding(prefix):
    launch()
    wait(lambda:exists("Get started"))
    snap(prefix+"-welcome");passed(prefix+" disabled IME welcome")
    tap("Get started");wait(lambda:exists("Enable in Settings"))
    passed(prefix+" welcome action")
    adb("shell","ime","enable",IME)
    launch();wait(lambda:exists("Switch input methods"))
    snap(prefix+"-enabled");passed(prefix+" enabled not selected")
    adb("shell","ime","set",IME)
    launch();wait(lambda:exists("Zero Voice & Glide"))
    snap(prefix+"-settings");passed(prefix+" selected settings")
    tap("Zero Voice & Glide");wait(lambda:exists("Enable offline voice"))
    snap(prefix+"-voice");passed(prefix+" voice settings")
    adb("shell","input","keyevent","4")
try:
    adb("logcat","-c")
    # Update from actual previous release, with both APKs re-signed by the same ephemeral CI certificate.
    adb("install",sys.argv[1])
    adb("install","-r",sys.argv[2])
    package_dump=adb("shell","dumpsys","package",PKG)
    assert "versionCode=103" in package_dump
    onboarding("upgrade")
    adb("uninstall",PKG)
    adb("install",sys.argv[2])
    onboarding("fresh")
    logs=adb("logcat","-d","-s","AndroidRuntime:E")
    (out/"android-runtime.txt").write_text(logs)
    assert not re.search(r"Process: com\.zerokeyboard\.ime(?:,|:)",logs),logs
    report["checks"].append("no Zero AndroidRuntime crash")
except Exception as exc:
    report["error"]=str(exc)
    try:snap("failure")
    except Exception:pass
    raise
finally:
    try:(out/"android-runtime.txt").write_text(adb("logcat","-d","-s","AndroidRuntime:E"))
    except Exception:pass
    (out/"report.json").write_text(json.dumps(report,ensure_ascii=False,indent=2))
