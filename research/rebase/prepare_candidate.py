"""Produce a deterministic Zero candidate from the pinned upstream checkout.
Never touch the original app or private signing key. Fail on unexpected source drift.
"""
import argparse
import re
import subprocess
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

PIN = "f8ec91a982835911cc224fa99434c3396d4cd562"
PACKAGE = "com.zerokeyboard.ime"
NS = "{http://schemas.android.com/apk/res/android}"
VERSION_CODE = 105
VERSION_NAME = "0.2.3-engine-preview"

def replace(path, before, after, count=1):
    text = path.read_text(encoding="utf-8")
    if text.count(before) != count:
        raise ValueError(f"unexpected source match count: {path.name}: {before[:70]!r}")
    path.write_text(text.replace(before, after), encoding="utf-8")

def prepare(upstream, zero):
    assert subprocess.check_output(["git","-C",str(upstream),"rev-parse","HEAD"],text=True).strip()==PIN
    app = upstream / "app"
    java = app / "src/main/java/helium314/keyboard/latin"
    gradle = app / "build.gradle.kts"
    for before,after in [
        ('applicationId = "helium314.keyboard"','applicationId = "com.zerokeyboard.ime"'),
        ('minSdk = 21','minSdk = 29'),
        ('versionCode = 3603',f'versionCode = {VERSION_CODE}'),
        ('versionName = "3.6"',f'versionName = "{VERSION_NAME}"'),
        ('listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")','listOf("arm64-v8a", "x86_64")'),
    ]:
        replace(gradle,before,after)
    manifest = app / "src/main/AndroidManifest.xml"
    replace(manifest,'android:name="LatinIME"','android:name="com.zerokeyboard.ime.ZeroImeService"')
    replace(manifest,'android:allowBackup="true"','android:allowBackup="false"')
    # Public Android SDK cannot construct a CE context from a DE-default app.
    # Use ordinary credential-protected application storage, as Alpha01/02 did.
    replace(manifest,'android:defaultToDeviceProtectedStorage="true"','android:defaultToDeviceProtectedStorage="false"')
    replace(manifest,'android:directBootAware="true"','android:directBootAware="false"',2)
    replace(manifest,'    <uses-permission android:name="android.permission.READ_CONTACTS" />\n','')
    # A delayed package/boot/locale broadcast can arrive while SettingsActivity is visible.
    # Let Android manage the process lifecycle for the installed, user-facing app.
    replace(manifest, '        <receiver android:name="SystemBroadcastReceiver"\n                android:exported="true">',
            '        <receiver android:name="SystemBroadcastReceiver"\n                android:exported="false">')
    receiver = java / "SystemBroadcastReceiver.java"
    for import_line in (
        "import android.os.Process;\n",
        "import android.view.inputmethod.InputMethodManager;\n",
        "import helium314.keyboard.latin.utils.UncachedInputMethodManagerUtils;\n",
    ):
        replace(receiver, import_line, "")
    replace(receiver, """
        // The process that hosts this broadcast receiver is invoked and remains alive even after
        // 1) the package has been re-installed,
        // 2) the device has just booted,
        // 3) a new user has been created.
        // There is no good reason to keep the process alive if this IME isn't a current IME.
        final InputMethodManager imm = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        // Called to check whether this IME has been triggered by the current user or not
        final boolean isInputMethodManagerValidForUserOfThisProcess =
                !imm.getInputMethodList().isEmpty();
        final boolean isCurrentImeOfCurrentUser = isInputMethodManagerValidForUserOfThisProcess
                && UncachedInputMethodManagerUtils.isThisImeCurrent(context, imm);
        if (!isCurrentImeOfCurrentUser) {
            final int myPid = Process.myPid();
            Log.i(TAG, "Killing my process: pid=" + myPid);
            Process.killProcess(myPid);
        }
""", "")
    # Keep label resource references: WelcomeWizard previously read labelRes, which is zero for literal labels.
    replace(app/"src/main/res/values/donottranslate.xml",'<string name="english_ime_name" translatable="false">HeliBoard</string>','<string name="english_ime_name" translatable="false">Zero Keyboard</string>')
    replace(app/"src/main/res/values/strings.xml",'<string name="ime_settings">HeliBoard Settings</string>','<string name="ime_settings">Zero Keyboard Settings</string>')
    wizard=app/"src/main/java/helium314/keyboard/settings/WelcomeWizard.kt"
    replace(wizard,"val appName = stringResource(ctx.applicationInfo.labelRes)",'val appName = ctx.applicationInfo.loadLabel(ctx.packageManager).toString().ifBlank { "Zero Keyboard" }')
    wrapper = app / "src/main/java/com/zerokeyboard/ime/ZeroImeService.java"
    wrapper.parent.mkdir(parents=True,exist_ok=True)
    wrapper.write_text("""// SPDX-License-Identifier: GPL-3.0-only
package com.zerokeyboard.ime;
/** Keep Android's existing IME component identity across the engine change. */
public final class ZeroImeService extends helium314.keyboard.latin.LatinIME {}
""",encoding="utf-8")
    # Share the existing editor and plain-text policies with the mature clipboard.
    shutil.copy2(zero/"app/src/main/java/com/zerokeyboard/ime/EditorPolicy.java", wrapper.parent/"EditorPolicy.java")
    for qualifier in ("values", "values-ar"):
        shutil.copy2(zero / "research/rebase/res" / qualifier / "zero_strings.xml",
                     app / "src/main/res" / qualifier / "zero_strings.xml")
    (wrapper.parent/"tools").mkdir(exist_ok=True)
    shutil.copy2(zero/"app/src/main/java/com/zerokeyboard/ime/tools/ClipboardHistory.java",
                 wrapper.parent/"tools/ClipboardHistory.java")
    clipboard = java / "ClipboardHistoryManager.kt"
    replace(clipboard, "import android.text.InputType\n", "")
    replace(clipboard, "import android.text.TextUtils\n", "")
    replace(clipboard, "import helium314.keyboard.latin.common.isValidNumber\n", "")
    replace(clipboard, "import helium314.keyboard.latin.utils.InputTypeUtils\n",
            "import com.zerokeyboard.ime.EditorPolicy\nimport com.zerokeyboard.ime.tools.ClipboardHistory\n")
    replace(clipboard,
            "    private fun fetchPrimaryClip() {\n        val clipData = clipboardManager.primaryClip ?: return",
            """    fun allowsClipboardHistory() = allowsClipboardPreview(latinIME.currentInputEditorInfo)

    private fun allowsClipboardPreview(editorInfo: EditorInfo?) =
        EditorPolicy.from(editorInfo).allowPersonalization && !latinIME.mSettings.current.mIncognitoModeEnabled

    private fun fetchPrimaryClip() {
        if (!allowsClipboardHistory()) return
        val clipData = clipboardManager.primaryClip ?: return
        if (ClipboardManagerCompat.getClipSensitivity(clipData.description) == true) return""")
    replace(clipboard, """            val content = clipItem.coerceToText(latinIME)
            if (TextUtils.isEmpty(content)) return
            clipboardDao?.addClip(timeStamp, false, content.toString())""",
            """            val content = clipItem.text?.toString() ?: return
            if (!ClipboardHistory.acceptable(content, false)) return
            clipboardDao?.addClip(timeStamp, false, content)""")
    replace(clipboard, """    private fun isClipSensitive(inputType: Int): Boolean {
        ClipboardManagerCompat.getClipSensitivity(clipboardManager.primaryClip?.description)?.let { return it }
        return InputTypeUtils.isPasswordInputType(inputType)
    }

""", "")
    replace(clipboard, "        if (!latinIME.mSettings.current.mSuggestClipboardContent) return null",
            "        if (!allowsClipboardPreview(editorInfo) || !latinIME.mSettings.current.mSuggestClipboardContent) return null")
    replace(clipboard, "        val clipItem = clipData.getItemAt(0) ?: return null",
            """        if (ClipboardManagerCompat.getClipSensitivity(clipData.description) == true) return null
        val clipItem = clipData.getItemAt(0) ?: return null""")
    replace(clipboard, """        val content = clipItem.coerceToText(latinIME)
        if (TextUtils.isEmpty(content)) return null
        val inputType = editorInfo?.inputType ?: InputType.TYPE_NULL
        if (InputTypeUtils.isNumberInputType(inputType) && !content.isValidNumber()) return null""",
            """        val content = clipItem.text?.toString() ?: return null
        if (!ClipboardHistory.acceptable(content, false)) return null""")
    replace(clipboard, """        textView.text = (if (isClipSensitive(inputType)) "*".repeat(content.length) else content)
            .take(200) // truncate displayed text for performance reasons""",
            """        textView.text = content.take(200)""")
    replace(clipboard, "        textView.setOnClickListener {",
            """        textView.setOnClickListener {
            if (!allowsClipboardHistory()) return@setOnClickListener""")
    replace(clipboard, "fun getHistorySize() = clipboardDao?.count() ?: 0",
            "fun getHistorySize() = if (allowsClipboardHistory()) clipboardDao?.count() ?: 0 else 0")
    replace(clipboard, "fun getHistoryEntry(position: Int) = clipboardDao?.getAt(position)",
            "fun getHistoryEntry(position: Int) = if (allowsClipboardHistory()) clipboardDao?.getAt(position) else null")
    replace(clipboard, "fun getHistoryEntryContent(id: Long) = clipboardDao?.get(id)",
            "fun getHistoryEntryContent(id: Long) = if (allowsClipboardHistory()) clipboardDao?.get(id) else null")
    keyboard = app / "src/main/java/helium314/keyboard/keyboard"
    switcher = keyboard / "KeyboardSwitcher.java"
    replace(switcher, """    @Override
    public void setClipboardKeyboard() {""",
            """    @Override
    public boolean canShowClipboardHistory() {
        return mLatinIME.getClipboardHistoryManager().allowsClipboardHistory();
    }

    @Override
    public void setClipboardKeyboard() {
        if (!canShowClipboardHistory()) return;""")
    history_view = keyboard / "clipboard/ClipboardHistoryView.kt"
    replace(history_view, "        keyboardActionListener.onTextInput(clipContent?.text)",
            "        clipContent?.let { keyboardActionListener.onTextInput(it.text) }")
    state = keyboard / "internal/KeyboardState.kt"
    replace(state, "        fun setClipboardKeyboard()",
            "        fun canShowClipboardHistory(): Boolean\n        fun setClipboardKeyboard()")
    replace(state, "    private fun setClipboardKeyboard() {",
            """    private fun setClipboardKeyboard() {
        if (!mSwitchActions.canShowClipboardHistory()) return""")
    defaults = java / "settings/Defaults.kt"
    for key,value in {
        "PREF_AUTO_CORRECTION":"false","PREF_SUGGEST_CLIPBOARD_CONTENT":"false",
        "PREF_ENABLE_CLIPBOARD_HISTORY":"false","PREF_SHOW_LANGUAGE_SWITCH_KEY":"true","PREF_SHOW_EMOJI_KEY":"true",
        "PREF_GESTURE_INPUT":"false","PREF_ALWAYS_INCOGNITO_MODE":"false",
        "PREF_KEY_USE_PERSONALIZED_DICTS":"false",
        "PREF_USE_CONTACTS":"false",
        # Owner's Gboard reference: clean key faces, always-visible digits,
        # suggestions without forced correction, and no double-space period.
        "PREF_SHOW_NUMBER_ROW":"true","PREF_SHOW_HINTS":"false",
        "PREF_KEY_USE_DOUBLE_SPACE_PERIOD":"false","PREF_SOUND_ON":"true"
    }.items():
        source=defaults.read_text(encoding="utf-8")
        source,n=re.subn(r"(?m)^(\s*const val "+key+r" = )(?:true|false)$",lambda m:m[1]+value,source)
        if n!=1: raise ValueError("unexpected default: "+key)
        defaults.write_text(source,encoding="utf-8")
    replace(defaults, "const val PREF_KEYPRESS_SOUND_VOLUME = -0.01f", "const val PREF_KEYPRESS_SOUND_VOLUME = 0.19f")
    replace(defaults, "var PREF_POPUP_ON = true", "val PREF_POPUP_ON = true")
    replace(defaults, """        val dm = context.resources.displayMetrics
        val px600 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 600f, dm)
        PREF_POPUP_ON = dm.widthPixels >= px600 || dm.heightPixels >= px600""",
            """        PREF_KEYBOARD_HEIGHT_SCALE[0] =
            if (context.resources.configuration.smallestScreenWidthDp >= 600) 0.82f else 1.0f""")
    replace(defaults, "import android.util.TypedValue\n", "")
    for key, old, new in [
        ("PREF_THEME_STYLE", "STYLE_MATERIAL", "STYLE_ROUNDED"),
        ("PREF_THEME_COLORS", "THEME_LIGHT", "THEME_ZERO_LIGHT"),
        ("PREF_THEME_COLORS_NIGHT", "THEME_DARK", "THEME_ZERO_DARK"),
    ]:
        replace(defaults, f"const val {key} = KeyboardTheme.{old}",
                f"const val {key} = KeyboardTheme.{new}")
    theme = keyboard / "KeyboardTheme.kt"
    replace(theme, '        const val THEME_LIGHT = "light"',
            '''        const val THEME_ZERO_LIGHT = "zero_glass_light"
        const val THEME_ZERO_DARK = "zero_glass_dark"
        const val THEME_LIGHT = "light"''')
    replace(theme, '            if (!isNight) THEME_LIGHT else null, THEME_DARK,',
            '''            if (!isNight) THEME_ZERO_LIGHT else null, THEME_ZERO_DARK,
            if (!isNight) THEME_LIGHT else null, THEME_DARK,''')
    replace(theme, "                THEME_LIGHT -> DefaultColors(",
            '''                THEME_ZERO_LIGHT -> DefaultColors(
                    themeStyle, hasBorders, 0xFF2256B5.toInt(), 0xFFE9EEF7.toInt(),
                    0xFFFAFCFF.toInt(), 0xFFD6E3F7.toInt(), 0xFFF9FBFF.toInt(),
                    0xFF19283D.toInt(), 0xFF4B5B71.toInt(),
                    keyboardBackground = backgroundImage
                )
                THEME_ZERO_DARK -> DefaultColors(
                    themeStyle, hasBorders, 0xFF9EC3FF.toInt(), 0xFF121A29.toInt(),
                    0xFF26344A.toInt(), 0xFF30486B.toInt(), 0xFF243752.toInt(),
                    0xFFF2F6FF.toInt(), 0xFFB7C6DF.toInt(),
                    keyboardBackground = backgroundImage
                )
                THEME_LIGHT -> DefaultColors(''')
    source=defaults.read_text(encoding="utf-8")
    source,n=re.subn(r'(?m)^    const val PREF_ADDITIONAL_SUBTYPES = .*?\n            .*?\n            .*?$', '    const val PREF_ADDITIONAL_SUBTYPES = ""',source)
    if n!=1: raise ValueError("additional subtype defaults changed")
    defaults.write_text(source,encoding="utf-8")
    subtype=java/"utils/SubtypeSettings.kt"
    replace(subtype,"getResourceSubtypes(resources).forEach {",'getResourceSubtypes(resources).filter { it.locale().language in setOf("ar", "en") }.forEach {')
    replace(subtype,"additionalSubtypes.addAll(subtypes)",'additionalSubtypes.addAll(subtypes.filter { it.locale().language in setOf("ar", "en") })')
    replace(subtype,"    fun addEnabledSubtype(prefs: SharedPreferences, newSubtype: InputMethodSubtype) {",'    fun addEnabledSubtype(prefs: SharedPreferences, newSubtype: InputMethodSubtype) {\n        if (newSubtype.locale().language !in setOf("ar", "en")) return')
    source=subtype.read_text(encoding="utf-8")
    start=source.index("        val subtypes = systemLocales.mapNotNull")
    end=source.index("        return systemSubtypes",start)
    source=source[:start]+'''        systemSubtypes.add(resourceSubtypesByLocale[Locale.US]!!.first())
        val arabic = resourceSubtypesByLocale.entries.firstOrNull { it.key.language == "ar" }?.value?.firstOrNull()
        if (arabic != null) systemSubtypes.add(arabic)
'''+source[end:]
    subtype.write_text(source,encoding="utf-8")
    # The upstream globe visibility checks explicitly enabled subtypes (fallback=false).
    # Seed both languages even when the preference is empty or only one was enabled.
    replace(subtype,"            enabledSubtypes.add(subtype)\n        }\n    }",'''            enabledSubtypes.add(subtype)
        }
        enabledSubtypes.removeAll { it.locale().language !in setOf("ar", "en") }
        for (language in listOf("en", "ar")) {
            if (enabledSubtypes.none { it.locale().language == language }) {
                val fallback = if (language == "en") resourceSubtypesByLocale[Locale.US]?.firstOrNull()
                    else resourceSubtypesByLocale.entries.firstOrNull { it.key.language == language }?.value?.firstOrNull()
                checkNotNull(fallback) { "Zero required language missing: $language" }
                enabledSubtypes.add(fallback)
            }
        }
    }

    fun seedZeroLanguages(context: Context) {
        reloadEnabledSubtypes(context)
        val prefs = context.prefs()
        val value = createPrefSubtypes(enabledSubtypes.map { it.toSettingsSubtype() })
        if (prefs.getString(Settings.PREF_ENABLED_SUBTYPES, "") != value) {
            prefs.edit { putString(Settings.PREF_ENABLED_SUBTYPES, value) }
        }
    }''')
    appClass = java/"App.kt"
    replace(appClass,"        AppUpgrade.checkVersionUpgrade(this)","        AppUpgrade.checkVersionUpgrade(this)\n        SubtypeSettings.seedZeroLanguages(this)")
    # Keep upstream data schema version independent of Zero's smaller APK version.
    upgrade = java/"AppUpgrade.kt"
    replace(upgrade,"BuildConfig.VERSION_CODE","3603",2)
    jni=java/"utils/JniUtils.java"
    source=jni.read_text(encoding="utf-8")
    start=source.index("    static {")
    end=source.index("    private JniUtils()",start)
    source=source[:start]+'''    static {
        // Zero: never load proprietary or user-imported executable libraries.
        System.loadLibrary(JNI_LIB_NAME);
        sHaveGestureLib = false;
    }

'''+source[end:]
    jni.write_text(source,encoding="utf-8")
    # Block upstream shortcut IME dispatch until our reviewed local voice bridge is ready.
    ime=java/"LatinIME.java"
    replace(ime,'            mRichImm.switchToShortcutIme(this);','            return;')
    attr=java/"InputAttributes.java"
    replace(attr,'mShouldShowVoiceInputKey = !noMicrophone;','mShouldShowVoiceInputKey = false;')
    replace(attr, "            mNoLearning = false;\n            return;",
            "            mNoLearning = true;\n            return;")
    replace(attr, """        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            mNoLearning = (editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0;
        else
            mNoLearning = false;""",
            """        mNoLearning = !com.zerokeyboard.ime.EditorPolicy.from(editorInfo).allowPersonalization;""")
    replace(attr, """        return editorInfo.inputType == mInputType && mEditorInfo != null
                && (mEditorInfo.imeOptions & EditorInfo.IME_FLAG_FORCE_ASCII) == (editorInfo.imeOptions & EditorInfo.IME_FLAG_FORCE_ASCII);""",
            """        return editorInfo.inputType == mInputType && mEditorInfo != null
                && mNoLearning == (!com.zerokeyboard.ime.EditorPolicy.from(editorInfo).allowPersonalization)
                && java.util.Objects.equals(mEditorInfo.privateImeOptions, editorInfo.privateImeOptions)
                && (mEditorInfo.imeOptions & EditorInfo.IME_FLAG_FORCE_ASCII) == (editorInfo.imeOptions & EditorInfo.IME_FLAG_FORCE_ASCII);""")
    logic = java / "inputlogic/InputLogic.java"
    replace(logic, """        if (settingsValues.mIncognitoModeEnabled) {
            // still adjust confidences, otherwise incognito input fields can be very annoying when wrong language is active
            mDictionaryFacilitator.adjustConfidences(word, wasAutoCapitalized);
            return;
        }""", """        if (settingsValues.mIncognitoModeEnabled) return;""")
    # Disable the upstream in-app and logcat logger. Native/direct logs still need review.
    log=java/"utils/Log.kt"
    source=log.read_text(encoding="utf-8")
    source=re.sub(r"(?m)^        (?:log\(LogLine|android\.util\.Log\.).*$","        // Zero: no keyboard event logging.",source)
    log.write_text(source,encoding="utf-8")
    # Preserve prior app files and voice models: no destructive migration.
    # Copy the voice integration source into a separate staged module, not the candidate APK yet.
    staged=upstream/"zero-staged-voice"
    shutil.copytree(zero/"app/src/main/java/com/zerokeyboard/ime/voice",staged/"java/com/zerokeyboard/ime/voice")
    shutil.copytree(zero/"app/src/main/cpp",staged/"cpp")
    # Build-time source license and exact provenance accompany this candidate.
    (upstream/"ZERO-PROVENANCE.txt").write_text(
        "Zero Keyboard engine evaluation\nUpstream HeliBoard commit "+PIN+
        "\nGPL-3.0-only derivative; retain upstream notices and complete corresponding source.\n"
        "Zero changes: package/component identity, two-language restriction, privacy defaults, no external voice or proprietary glide.\n"
        "Staged Zero voice code is not built or active in this candidate.\n",encoding="utf-8")
    root=ET.parse(manifest).getroot()
    permissions={n.get(NS+"name") for n in root.findall("uses-permission")}
    assert "android.permission.INTERNET" not in permissions
    assert "android.permission.READ_CONTACTS" not in permissions
    assert root.find("application").get(NS+"allowBackup")=="false"
    assert root.find("application").get(NS+"label")=="@string/english_ime_name"
    assert "stringResource(ctx.applicationInfo.labelRes)" not in wizard.read_text()
    assert root.find("application").get(NS+"defaultToDeviceProtectedStorage")=="false"
    assert root.find("application").get(NS+"directBootAware")=="false"
    assert any(n.get(NS+"name")==PACKAGE+".ZeroImeService" for n in root.findall("application/service"))
    print("Prepared pinned mature core candidate; voice staged, not integrated; no signing key used.")

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("upstream",type=Path)
    parser.add_argument("zero",type=Path)
    args=parser.parse_args()
    prepare(args.upstream.resolve(),args.zero.resolve())
