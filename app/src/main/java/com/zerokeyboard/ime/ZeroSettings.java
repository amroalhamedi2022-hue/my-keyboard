package com.zerokeyboard.ime;
import android.content.Context;
import android.content.SharedPreferences;
final class ZeroSettings {
 private final SharedPreferences p;
 ZeroSettings(Context c){p=c.getSharedPreferences("zero_settings",Context.MODE_PRIVATE);}
 boolean glide(boolean arabic){return p.getBoolean(arabic?"glide_ar":"glide_en",true);}
 boolean trail(){return p.getBoolean("glide_trail",true);}
 boolean space(){return p.getBoolean("glide_space",true);}
 boolean incognito(){return p.getBoolean("incognito",false);}
 boolean voice(){return p.getBoolean("voice_enabled",false);}
 com.zerokeyboard.ime.voice.ModelCatalog model(){return p.getBoolean("voice_base",false)?com.zerokeyboard.ime.voice.ModelCatalog.BASE:com.zerokeyboard.ime.voice.ModelCatalog.TINY;}
 boolean numberRow(){return p.getBoolean("number_row",false);}
 boolean indic(){return p.getBoolean("arabic_indic",false);}
 boolean editorTools(){return p.getBoolean("editor_tools",true);}
 boolean spaceCursor(){return p.getBoolean("space_cursor",true);}
 boolean inlineAutofill(){return p.getBoolean("inline_autofill",true);}
 boolean stopOnSilence(){return p.getBoolean("voice_endpoint",false);}
 boolean voiceReview(){return p.getBoolean("voice_review",true);}
 LayoutProfile.Mode layout(boolean tablet,boolean landscape){try{return LayoutProfile.Mode.valueOf(p.getString(profileKey(tablet,landscape)+"_mode","FULL"));}catch(IllegalArgumentException e){return LayoutProfile.Mode.FULL;}}
 int height(boolean tablet,boolean landscape){return p.getInt(profileKey(tablet,landscape)+"_height",100);}
 private String profileKey(boolean tablet,boolean landscape){return (tablet?"tablet":"phone")+(landscape?"_landscape":"_portrait");}
 void layout(boolean tablet,boolean landscape,LayoutProfile.Mode mode){p.edit().putString(profileKey(tablet,landscape)+"_mode",mode.name()).apply();}
 void height(boolean tablet,boolean landscape,int value){p.edit().putInt(profileKey(tablet,landscape)+"_height",value).apply();}
 void set(String key,boolean value){p.edit().putBoolean(key,value).apply();}
}
