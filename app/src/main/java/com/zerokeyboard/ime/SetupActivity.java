package com.zerokeyboard.ime;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
public final class SetupActivity extends Activity {
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
  int pad=(int)(24*getResources().getDisplayMetrics().density); root.setPadding(pad,pad,pad,pad);
  TextView title=new TextView(this); title.setText("Zero Keyboard"); title.setTextSize(30); root.addView(title);
  TextView info=new TextView(this); info.setText("Development baseline • Arabic / English\nOffline manual typing. Experimental glide uses a small built-in word list. Voice is not available yet."); root.addView(info);
  Button enable=new Button(this); enable.setText("1. Enable Zero Keyboard"); enable.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))); root.addView(enable);
  Button select=new Button(this); select.setText("2. Choose keyboard"); select.setOnClickListener(v->getSystemService(InputMethodManager.class).showInputMethodPicker()); root.addView(select);
  EditText test=new EditText(this); test.setHint("Try typing / جرّب الكتابة"); test.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE); root.addView(test);
  ZeroSettings settings=new ZeroSettings(this);
  toggle(root,"English glide",settings.glide(false),settings,"glide_en");
  toggle(root,"Arabic glide",settings.glide(true),settings,"glide_ar");
  toggle(root,"Glide trail",settings.trail(),settings,"glide_trail");
  toggle(root,"Glide automatic space",settings.space(),settings,"glide_space");
  toggle(root,"Incognito (disables glide)",settings.incognito(),settings,"incognito");
  ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
 }
 private void toggle(LinearLayout root,String title,boolean initial,ZeroSettings settings,String key){
  Switch item=new Switch(this);item.setText(title);item.setChecked(initial);item.setOnCheckedChangeListener((v,value)->settings.set(key,value));root.addView(item);
 }
}
