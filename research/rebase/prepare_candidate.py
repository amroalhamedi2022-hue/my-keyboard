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
        ('versionCode = 3603','versionCode = 103'),
        ('versionName = "3.6"','versionName = "0.2.1-engine-preview"'),
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
    defaults = java / "settings/Defaults.kt"
    for key,value in {
        "PREF_AUTO_CORRECTION":"false","PREF_SUGGEST_CLIPBOARD_CONTENT":"false",
        "PREF_ENABLE_CLIPBOARD_HISTORY":"false","PREF_SHOW_LANGUAGE_SWITCH_KEY":"true","PREF_SHOW_EMOJI_KEY":"true",
        "PREF_GESTURE_INPUT":"false","PREF_ALWAYS_INCOGNITO_MODE":"false",
        "PREF_KEY_USE_PERSONALIZED_DICTS":"false",
        "PREF_USE_CONTACTS":"false"
    }.items():
        source=defaults.read_text(encoding="utf-8")
        source,n=re.subn(r"(?m)^(\s*const val "+key+r" = )(?:true|false)$",lambda m:m[1]+value,source)
        if n!=1: raise ValueError("unexpected default: "+key)
        defaults.write_text(source,encoding="utf-8")
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
    # Disable the upstream in-app and logcat logger. Native/direct logs still need review.
    log=java/"utils/Log.kt"
    source=log.read_text(encoding="utf-8")
    source=re.sub(r"(?m)^        (?:log\(LogLine|android\.util\.Log\.).*$","        // Zero: no keyboard event logging.",source)
    log.write_text(source,encoding="utf-8")
    # Preserve prior app files and voice models: no destructive migration.
    # Copy the voice integration source into a separate staged module, not the candidate APK yet.
    staged=upstream/"zero-staged-voice"
    shutil.copytree(zero/"app/src/main/java/com/zerokeyboard/ime/voice",staged/"java/com/zerokeyboard/ime/voice")
    shutil.copy2(zero/"app/src/main/java/com/zerokeyboard/ime/EditorPolicy.java",staged/"java/com/zerokeyboard/ime/EditorPolicy.java")
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
