"""Fast preflight on prepared source; no hidden storage API calls or boot-time CE access."""
import sys
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(sys.argv[1])
ns="{http://schemas.android.com/apk/res/android}"
app=ET.parse(root/"app/src/main/AndroidManifest.xml").getroot().find("application")
assert app.get(ns+"defaultToDeviceProtectedStorage")=="false"
assert app.get(ns+"directBootAware")=="false"
for svc in app.findall("service"):
    assert svc.get(ns+"directBootAware","false")=="false"
for directory in [root/"app/src/main/java/com/zerokeyboard", root/"zero-voice/src/main/java"]:
    if directory.exists():
        for path in directory.rglob("*.java"):
            assert "createCredentialProtectedStorageContext(" not in path.read_text(),str(path)
print("PASS: credential-protected storage manifest and public API preflight")
