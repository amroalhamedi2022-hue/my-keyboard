"""Apply the Zero voice adapter to the prepared mature core (separate opt-in build)."""
import sys
from pathlib import Path
import shutil
from prepare_candidate import replace

upstream=Path(sys.argv[1]).resolve()
zero=Path(sys.argv[2]).resolve()
stage=upstream/"zero-staged-voice"
templates=zero/"research/rebase/voice"
module=upstream/"zero-voice"
shutil.copytree(stage/"java",module/"src/main/java")
# App manifest now uses normal CE storage. Keep original public Context API in ModelStore.
shutil.copytree(stage/"cpp",module/"src/main/cpp")
shutil.copy2(templates/"zero-voice.gradle",module/"build.gradle")
(module/"src/main/AndroidManifest.xml").write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android"/>\n')
with (upstream/"settings.gradle").open("a") as f:f.write("\ninclude ':zero-voice'\n")
app=upstream/"app"
replace(app/"build.gradle.kts", 'create("debugNoMinify") { // for faster builds in IDE', 'create("debugNoMinify") { // for faster builds in IDE\n            matchingFallbacks += listOf("debug")')
replace(app/"build.gradle.kts","dependencies {",'dependencies {\n    implementation(project(":zero-voice"))')
for name in ["ZeroImeService.java","VoiceSetupActivity.java"]:
    shutil.copy2(templates/name,app/"src/main/java/com/zerokeyboard/ime"/name)
manifest=app/"src/main/AndroidManifest.xml"
replace(manifest,'    <uses-permission android:name="android.permission.VIBRATE" />','    <uses-permission android:name="android.permission.VIBRATE" />\n    <uses-permission android:name="android.permission.RECORD_AUDIO" />')
replace(manifest,"        <!-- Activities -->",'''        <!-- Activities -->
        <activity android:name="com.zerokeyboard.ime.VoiceSetupActivity" android:exported="false" />''')
java=app/"src/main/java/helium314/keyboard/latin"
replace(java/"InputAttributes.java","mShouldShowVoiceInputKey = false;",'mShouldShowVoiceInputKey = false;',2)
# Replace only the final assignment, not the early invalid-editor branch.
path=java/"InputAttributes.java"
text=path.read_text()
needle="        mShouldShowVoiceInputKey = false;"
i=text.rindex(needle)
text=text[:i]+text[i:].replace(needle,"        mShouldShowVoiceInputKey = com.zerokeyboard.ime.EditorPolicy.from(editorInfo).allowPersonalization && !hasNoMicrophoneKeyOption();",1)
path.write_text(text)
with (upstream/"ZERO-PROVENANCE.txt").open("a") as f:f.write("Voice adapter enabled: Zero local JNI library, private model storage, editor-bound recording and preview. Runtime verification pending.\n")
print("Zero local voice module and service adapter attached.")
