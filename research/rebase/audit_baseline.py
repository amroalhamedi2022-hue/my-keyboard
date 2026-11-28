"""Source compatibility checks for an isolated upstream IME baseline."""
import json
import subprocess
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(sys.argv[1])
out = Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)
sha = subprocess.check_output(["git", "-C", str(root), "rev-parse", "HEAD"], text=True).strip()
assert sha == "f8ec91a982835911cc224fa99434c3396d4cd562"
ns = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
permissions = sorted(x.attrib.get(ns+"name","") for x in manifest.findall("uses-permission"))
subtypes = ET.parse(root / "app/src/main/res/xml/method.xml").getroot()
languages = sorted(set(x.attrib.get(ns+"languageTag",x.attrib.get(ns+"imeSubtypeLocale","")) for x in subtypes.iter("subtype")))
assert "android.permission.INTERNET" not in permissions
assert any(x=="ar" or x.startswith("ar-") for x in languages), languages
assert any(x=="en" or x.startswith("en-") for x in languages), languages
assert "GNU GENERAL PUBLIC LICENSE" in (root/"LICENSE").read_text()
report={"source_commit":sha,"arabic_english_subtypes":True,"manifest_permissions":permissions,"languages":languages,"migration_blockers":["GPLv3 compliance before distributing derivative","remove contacts and backup and audit direct boot storage","restrict language selection to Arabic/English","disable correction and learning by default","replace closed glide dependency with audited open engine","preserve Zero application ID and signing certificate and existing data","adapt Zero voice session and editor privacy gates"],"scope":"source compatibility; not runtime validation"}
(out/"source-audit.json").write_text(json.dumps(report,indent=2),encoding="utf-8")
print(json.dumps(report,indent=2))
