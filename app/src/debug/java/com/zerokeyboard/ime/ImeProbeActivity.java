package com.zerokeyboard.ime;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;

/** Debug-only, disposable emulator fixture. Never included in a release APK. */
public final class ImeProbeActivity extends Activity {
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setFocusableInTouchMode(true);
  root.setPadding(8,8,8,8);
  // Force feature availability so an absence assertion in a password field is meaningful.
  // Use only on an isolated test installation; these are debug preferences.
  ZeroSettings settings=new ZeroSettings(this);
  settings.set("voice_enabled",true);
  settings.set("glide_en",true);
  settings.set("glide_ar",true);
  settings.set("incognito",false);
  settings.set("editor_tools",true);
  settings.set("emoji_panel",true);
  settings.set("clipboard_history",true);
  android.content.ClipboardManager clipboard=getSystemService(android.content.ClipboardManager.class);
  if(clipboard!=null)clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Synthetic probe text"," probe"));
  field(root,"zero_probe_normal",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE,0);
  field(root,"zero_probe_password",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD,0);
  field(root,"zero_probe_numeric",InputType.TYPE_CLASS_NUMBER,0);
  field(root,"zero_probe_private",InputType.TYPE_CLASS_TEXT,EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
  setContentView(root);
  root.requestFocus();
 }
 private void field(LinearLayout root,String label,int type,int options) {
  EditText field=new EditText(this);
  field.setSingleLine(true);
  field.setInputType(type);
  field.setImeOptions(options);
  field.setContentDescription(label);
  field.setHint(label);
  root.addView(field,new LinearLayout.LayoutParams(-1,-2));
 }
}
