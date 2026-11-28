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
replace(app/"build.gradle.kts", 'create("runTests") { // build variant for running tests on CI that skips tests known to fail', 'create("runTests") { // build variant for running tests on CI that skips tests known to fail\n            matchingFallbacks += listOf("debug")')
replace(app/"build.gradle.kts", 'create("nouserlib") { // same as release, but does not allow the user to provide a library', 'create("nouserlib") { // same as release, but does not allow the user to provide a library\n            matchingFallbacks += listOf("release")')
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
text=text[:i]+text[i:].replace(needle,"        mShouldShowVoiceInputKey = com.zerokeyboard.ime.EditorPolicy.from(editorInfo).allowPersonalization\n                && !InputTypeUtils.isEmailVariation(variation) && !hasNoMicrophoneKeyOption();",1)
path.write_text(text)
# Popup and layout voice keys are local. Upstream only enables them for a system speech IME.
keyboard=app/"src/main/java/helium314/keyboard/keyboard"
replace(keyboard/"KeyboardSwitcher.java", "keyboardView.updateShortcutKey(mRichImm.isShortcutImeReady());",
        "keyboardView.updateShortcutKey(currentSettingsValues.mShowsVoiceInputKey && !currentSettingsValues.mIncognitoModeEnabled);")
replace(keyboard/"PopupKeysKeyboardView.java", "shortcutKey.setEnabled(RichInputMethodManager.getInstance().isShortcutImeReady());",
        """final var settings = helium314.keyboard.latin.settings.Settings.getValues();
            shortcutKey.setEnabled(settings.mShowsVoiceInputKey && !settings.mIncognitoModeEnabled);""")
strip=java/"suggestions/SuggestionStripView.kt"
replace(strip, "        val show = Settings.getValues().mShowsVoiceInputKey",
        """        val settings = Settings.getValues()
        val show = settings.mShowsVoiceInputKey && !settings.mIncognitoModeEnabled""")
# Make the local voice button visible without depending on an external speech IME.
toolbar=java/"utils/ToolbarUtils.kt"
replace(toolbar,'val defaultPinnedToolbarPref = entries.filterNot { it == CLOSE_HISTORY }.joinToString(Separators.ENTRY) {\n    it.name + Separators.KV + false\n}', 'val defaultPinnedToolbarPref = entries.filterNot { it == CLOSE_HISTORY }.joinToString(Separators.ENTRY) {\n    it.name + Separators.KV + (it == VOICE)\n}')
# Direct access from the mature settings home; does not depend on an editor or microphone button.
main=app/"src/main/java/helium314/keyboard/settings/screens/MainSettingsScreen.kt"
replace(main,"        val enabledSubtypes = SubtypeSettings.getEnabledSubtypes(true)", "        val enabledSubtypes = SubtypeSettings.getEnabledSubtypes(true)\n        val zeroContext = LocalContext.current")
replace(main,"                if (JniUtils.sHaveGestureLib)",'''                Preference(
                    name = "Zero Voice & Glide",
                    description = "Offline speech, models and open swipe typing",
                    onClick = { zeroContext.startActivity(android.content.Intent(zeroContext, com.zerokeyboard.ime.VoiceSetupActivity::class.java)) },
                    icon = R.drawable.ic_settings_gesture
                ) { NextScreenIcon() }
                if (JniUtils.sHaveGestureLib)''')
with (upstream/"ZERO-PROVENANCE.txt").open("a") as f:f.write("Voice adapter enabled: Zero local JNI library, private model storage, editor-bound recording and preview. Runtime verification pending.\n")
print("Zero local voice module and service adapter attached.")
