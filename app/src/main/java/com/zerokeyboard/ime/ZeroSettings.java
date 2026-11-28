package com.zerokeyboard.ime;
import android.content.Context;
import android.content.SharedPreferences;
import com.zerokeyboard.ime.glide.PersonalLexicon;
import java.util.*;
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
 boolean personalEnabled(){return p.getBoolean("personal_enabled",true);}
 String personalText(boolean ar){return p.getString(ar?"personal_ar":"personal_en","");}
 void personalText(boolean ar,String value){List<String> words=PersonalLexicon.parse(value,ar);p.edit().putString(ar?"personal_ar":"personal_en",String.join("\n",words)).apply();}
 List<String> personalWords(boolean ar){if(!personalEnabled())return Collections.emptyList();try{return PersonalLexicon.parse(personalText(ar),ar);}catch(IllegalArgumentException e){return Collections.emptyList();}}
 boolean clipboard(){return p.getBoolean("clipboard_history",false);}
 long clipboardTtl(){int min=p.getInt("clipboard_minutes",15);return Math.max(1,Math.min(60,min))*60000L;}
 void clipboardMinutes(int min){p.edit().putInt("clipboard_minutes",Math.max(1,Math.min(60,min))).apply();}
 boolean emoji(){return p.getBoolean("emoji_panel",true);}
 com.zerokeyboard.ime.voice.VoiceOptions voiceOptions(){return new com.zerokeyboard.ime.voice.VoiceOptions(p.getInt("voice_language",0),p.getInt("voice_threads",Math.min(4,Math.max(1,Runtime.getRuntime().availableProcessors()))),stopOnSilence(),p.getInt("voice_silence_ms",1200));}
 boolean continuousVoice(){return p.getBoolean("voice_continuous",false);}
 boolean importedDictionary(){return p.getBoolean("imported_dictionary",true);}
 void number(String key,int value){p.edit().putInt(key,value).apply();}
 void set(String key,boolean value){p.edit().putBoolean(key,value).apply();}
}
