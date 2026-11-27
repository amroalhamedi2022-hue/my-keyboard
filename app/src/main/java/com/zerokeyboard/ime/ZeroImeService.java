package com.zerokeyboard.ime;
import android.inputmethodservice.InputMethodService;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.List;

public final class ZeroImeService extends InputMethodService {
 private KeyboardLayout.Language lang=KeyboardLayout.Language.EN;
 private boolean shift=false,symbols=false;
 private LinearLayout root;
 private EditorPolicy policy=EditorPolicy.from(null);
 private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
 @Override public View onCreateInputView(){
  root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
  root.setPadding(dp(3),dp(4),dp(3),dp(5)); root.setBackgroundColor(Color.rgb(24,29,38)); render(); return root;
 }
 @Override public boolean onEvaluateFullscreenMode(){return false;}
 @Override public void onStartInput(EditorInfo info,boolean restarting){
  super.onStartInput(info,restarting); policy=EditorPolicy.from(info); shift=false; symbols=false; render();
 }
 @Override public void onStartInputView(EditorInfo info,boolean restarting){super.onStartInputView(info,restarting);policy=EditorPolicy.from(info);render();}
 @Override public void onFinishInput(){super.onFinishInput();policy=EditorPolicy.from(null);shift=false;symbols=false;}
 @Override public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype){
  super.onCurrentInputMethodSubtypeChanged(subtype);
  if(subtype!=null){lang=subtype.getLocale().startsWith("ar")?KeyboardLayout.Language.AR:KeyboardLayout.Language.EN;shift=false;symbols=false;render();}
 }
 @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);render();}
 private LinearLayout row(){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);root.addView(row,new LinearLayout.LayoutParams(-1,-2));return row;}
 private void key(LinearLayout row,String label,String desc,float weight,Runnable action){
  Button b=new Button(this); b.setAllCaps(false); b.setText(label); b.setContentDescription(desc);
  b.setTextColor(Color.WHITE);b.setTextSize(18);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(0,0,0,0);
  GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(49,58,73));bg.setCornerRadius(dp(8));b.setBackground(bg);
  int h=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?42:48;
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(h),weight);lp.setMargins(dp(2),dp(2),dp(2),dp(2));
  row.addView(b,lp);b.setOnClickListener(v->action.run());
 }
 private void render(){
  if(root==null)return;
  root.removeAllViews();
  for(String letters:KeyboardLayout.rows(lang,shift,symbols,policy.numeric)){
   LinearLayout line=row();for(String text:letters.split(" "))key(line,text,text,1f,()->insert(text));
  }
  LinearLayout controls=row();
  if(!policy.numeric){
   key(controls,lang==KeyboardLayout.Language.AR?"EN":"ع","Switch language",1.3f,()->{lang=lang==KeyboardLayout.Language.EN?KeyboardLayout.Language.AR:KeyboardLayout.Language.EN;shift=false;symbols=false;render();});
   key(controls,symbols?"ABC":"123","Symbols",1.3f,()->{symbols=!symbols;shift=false;render();});
   key(controls,"⇧","Shift or diacritics",1f,()->{shift=!shift;render();});
  }
  key(controls,"⌫","Delete",1.3f,this::delete);
  key(controls,lang==KeyboardLayout.Language.AR?"مسافة":"Space","Space",3f,()->insert(" "));
  key(controls,"↵","Editor action",1.3f,this::enter);
 }
 private void insert(String value){
  InputConnection c=getCurrentInputConnection();if(c==null)return;
  c.commitText(value,1);
  if(shift&&!symbols){shift=false;render();}
 }
 private void delete(){
  InputConnection c=getCurrentInputConnection();if(c==null)return;
  // Avoid reading the editor or materializing selected password text.
  if(c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DEL)))
   c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DEL));
  else c.deleteSurroundingTextInCodePoints(1,0);
 }
 private void enter(){
  InputConnection c=getCurrentInputConnection();EditorInfo e=getCurrentInputEditorInfo();if(c==null||e==null)return;
  if((e.imeOptions&EditorInfo.IME_FLAG_NO_ENTER_ACTION)==0){
   if(e.actionLabel!=null&&e.actionId!=0&&c.performEditorAction(e.actionId))return;
   int a=e.imeOptions&EditorInfo.IME_MASK_ACTION;
   if(a!=EditorInfo.IME_ACTION_NONE&&a!=EditorInfo.IME_ACTION_UNSPECIFIED&&c.performEditorAction(a))return;
  }
  if((e.inputType&android.text.InputType.TYPE_MASK_CLASS)==android.text.InputType.TYPE_CLASS_TEXT&&(e.inputType&android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE)!=0)c.commitText("\n",1);
  else {c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));}
 }
}
