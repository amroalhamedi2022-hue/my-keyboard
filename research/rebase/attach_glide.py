"""Attach opt-in open geometric glide. Do not use closed upstream gesture library."""
import sys
from pathlib import Path
import shutil
from prepare_candidate import replace
root=Path(sys.argv[1]);zero=Path(sys.argv[2])
app=root/"app";package=app/"src/main/java/com/zerokeyboard/ime"
glide=package/"glide";glide.mkdir(parents=True,exist_ok=True)
for name in ["GeometricRanker.java","FrequencyLexicon.java","PersonalLexicon.java","DictionaryResourceStore.java","SeedLexicon.java"]:
    shutil.copy2(zero/"app/src/main/java/com/zerokeyboard/ime/glide"/name,glide/name)
for name in ["GestureGate.java","MatureGlide.java"]:
    shutil.copy2(zero/"research/rebase/glide"/name,glide/name)
shutil.copytree(zero/"app/src/main/assets/dictionaries",app/"src/main/assets/dictionaries",dirs_exist_ok=True)
# Preserve CE resource path for legacy imported dictionaries.
replace(glide/"DictionaryResourceStore.java","context=c.getApplicationContext();","context=c.createCredentialProtectedStorageContext();")
view=app/"src/main/java/helium314/keyboard/keyboard/MainKeyboardView.java"
replace(view,"    public boolean onTouchEvent(final MotionEvent event) {","    public boolean onTouchEvent(final MotionEvent event) {\n        if (zeroGlide != null && zeroGlide.onTouch(this, event, isShowingPopupKeysPanel())) return true;")
replace(view,"    public boolean processMotionEvent(final MotionEvent event) {","    private com.zerokeyboard.ime.glide.MatureGlide zeroGlide;\n    public void setZeroGlide(com.zerokeyboard.ime.glide.MatureGlide glide) { zeroGlide = glide; }\n\n    public boolean processMotionEvent(final MotionEvent event) {")
service=package/"ZeroImeService.java"
replace(service,"    private VoiceSession voice;","    private VoiceSession voice;\n    private com.zerokeyboard.ime.glide.MatureGlide glide;")
replace(service,"super.onCreate(); voice=new VoiceSession();","""super.onCreate(); voice=new VoiceSession();
        glide=new com.zerokeyboard.ime.glide.MatureGlide(storage(),new com.zerokeyboard.ime.glide.MatureGlide.Host(){
            public boolean allowed(){return ZeroImeService.this.allowed()&&!voice.busy();}
            public SharedPreferences settings(){return prefs();}
            public InputConnection target(){return getCurrentInputConnection();}
            public void commit(InputConnection target,String word){if(allowed()&&target==getCurrentInputConnection())insertGlide(word);}
        });
        bindGlide();""")
replace(service,"super.setInputView(view); anchor=view;","super.setInputView(view); anchor=view; bindGlide();")
replace(service,"    private boolean allowed(){","""    private void bindGlide(){
        if(anchor!=null&&glide!=null){
            View v=anchor.findViewById(helium314.keyboard.latin.R.id.keyboard_view);
            if(v instanceof helium314.keyboard.keyboard.MainKeyboardView)((helium314.keyboard.keyboard.MainKeyboardView)v).setZeroGlide(glide);
        }
    }
    private void insertGlide(String word){cancelVoice();super.onTextInput(word);}
    private boolean allowed(){""")
replace(service,"        generation++;pending=","        if(glide!=null)glide.invalidate();\n        generation++;pending=")
replace(service,"if(voice!=null)voice.close(); super.onDestroy();","if(voice!=null)voice.close(); if(glide!=null)glide.close(); super.onDestroy();")
# Reset gesture state on editor/window changes (ordinary events invalidate only pending results).
for old in ["super.onStartInput(info,restarting);","super.onFinishInputView(finishing);","super.onFinishInput();","super.onWindowHidden();","super.onConfigurationChanged(c);","super.onCurrentInputMethodSubtypeChanged(subtype);"]:
    replace(service,old,"if(glide!=null)glide.cancel(); "+old)
setup=package/"VoiceSetupActivity.java"
replace(setup,'toggle(root,"Enable offline voice","voice_enabled",false);','toggle(root,"Experimental open glide (3+ distinct letters)","mature_glide_enabled",false);\n        toggle(root,"Enable offline voice","voice_enabled",false);')
with (root/"ZERO-PROVENANCE.txt").open("a") as f:f.write("Optional original geometric glide attached; CC BY-SA dictionaries preserved with NOTICE. Not neural or validated against real traces.\n")
