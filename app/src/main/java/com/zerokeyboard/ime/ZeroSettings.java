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
 void set(String key,boolean value){p.edit().putBoolean(key,value).apply();}
}
