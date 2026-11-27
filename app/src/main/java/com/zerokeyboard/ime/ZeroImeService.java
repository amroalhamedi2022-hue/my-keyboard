package com.zerokeyboard.ime;
import android.inputmethodservice.InputMethodService;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import com.zerokeyboard.ime.glide.GeometricRanker;
import com.zerokeyboard.ime.glide.SeedLexicon;
import java.util.*;
import android.Manifest;
import android.content.pm.PackageManager;
import com.zerokeyboard.ime.voice.*;

public final class ZeroImeService extends InputMethodService {
 private KeyboardLayout.Language lang=KeyboardLayout.Language.EN;
 private boolean shift=false,symbols=false,active=false;
 private GlideSurface root;
 private LinearLayout strip;
 private EditorPolicy policy=EditorPolicy.from(null);
 private ZeroSettings settings;
 private final VoiceSession voice=new VoiceSession();
 private ModelStore models;
 private Button voiceKey;
 private long editorGeneration;
 private boolean voiceProcessing;
 private final GeometricRanker ranker=new GeometricRanker();
 private InputConnection composingConnection;
 private String composed="";
 private boolean confirmed=false;
 private int composingStart=-1,composingEnd=-1;
 private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
 @Override public View onCreateInputView(){
  settings=new ZeroSettings(this);models=new ModelStore(this);root=new GlideSurface(this,this::glide);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
  root.setPadding(dp(3),dp(4),dp(3),dp(5));root.setBackgroundColor(Color.rgb(24,29,38));render();return root;
 }
 @Override public boolean onEvaluateFullscreenMode(){return false;}
 @Override public void onStartInput(EditorInfo info,boolean restarting){cancelVoice();clearComposition(false);super.onStartInput(info,restarting);active=true;policy=EditorPolicy.from(info);shift=false;symbols=false;render();}
 @Override public void onStartInputView(EditorInfo info,boolean restarting){super.onStartInputView(info,restarting);active=true;policy=EditorPolicy.from(info);render();}
 @Override public void onFinishInputView(boolean finishingInput){cancelVoice();clearComposition(true);active=false;if(root!=null)root.reset();super.onFinishInputView(finishingInput);}
 @Override public void onFinishInput(){cancelVoice();clearComposition(false);active=false;policy=EditorPolicy.from(null);shift=false;symbols=false;if(root!=null)root.reset();super.onFinishInput();}
 @Override public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype){
  super.onCurrentInputMethodSubtypeChanged(subtype);if(subtype!=null){cancelVoice();clearComposition(true);lang=subtype.getLocale().startsWith("ar")?KeyboardLayout.Language.AR:KeyboardLayout.Language.EN;shift=false;symbols=false;render();}
 }
 @Override public void onConfigurationChanged(Configuration config){cancelVoice();clearComposition(true);super.onConfigurationChanged(config);render();}
 @Override public void onWindowHidden(){cancelVoice();super.onWindowHidden();}
 @Override public void onDestroy(){voice.close();super.onDestroy();}
 private void cancelVoice(){editorGeneration++;voice.cancel();voiceProcessing=false;if(voiceKey!=null)voiceKey.setText("Mic");}
 private boolean voiceAllowed(){return active&&isInputViewShown()&&settings!=null&&settings.voice()&&!settings.incognito()&&policy.allowPersonalization&&!policy.sensitive;}
 private boolean glideAllowed(){return active&&settings!=null&&settings.glide(lang==KeyboardLayout.Language.AR)&&!settings.incognito()&&policy.allowPersonalization&&!shift&&!symbols&&!policy.numeric;}
 private LinearLayout row(){LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER);root.addView(line,new LinearLayout.LayoutParams(-1,-2));return line;}
 private Button key(LinearLayout row,String label,String desc,float weight,Runnable action){
  Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setContentDescription(desc);b.setTextColor(Color.WHITE);b.setTextSize(18);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(0,0,0,0);
  GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(49,58,73));bg.setCornerRadius(dp(8));b.setBackground(bg);
  int h=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?42:48;
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(h),weight);lp.setMargins(dp(2),dp(2),dp(2),dp(2));row.addView(b,lp);b.setOnClickListener(v->action.run());return b;
 }
 private void render(){
  if(root==null)return;root.configure(glideAllowed(),settings.trail());root.removeAllViews();strip=row();strip.setVisibility(glideAllowed()?View.VISIBLE:View.GONE);strip.setMinimumHeight(dp(48));
  for(String letters:KeyboardLayout.rows(lang,shift,symbols,policy.numeric)){
   LinearLayout line=row();for(String text:letters.split(" ")){Button b=key(line,text,text,1f,()->insert(text));root.register(b,text);}
  }
  LinearLayout controls=row();
  if(!policy.numeric){
   key(controls,lang==KeyboardLayout.Language.AR?"EN":"ع","Switch language",1.3f,()->{cancelVoice();clearComposition(true);lang=lang==KeyboardLayout.Language.EN?KeyboardLayout.Language.AR:KeyboardLayout.Language.EN;shift=false;symbols=false;render();});
   key(controls,symbols?"ABC":"123","Symbols",1.3f,()->{cancelVoice();clearComposition(true);symbols=!symbols;shift=false;render();});
   key(controls,"⇧","Shift or diacritics",1f,()->{cancelVoice();clearComposition(true);shift=!shift;render();});
  }
  voiceKey=null;if(active&&settings.voice()&&!settings.incognito()&&policy.allowPersonalization&&!policy.sensitive)voiceKey=key(controls,"Mic","Offline voice typing",1.5f,this::toggleVoice);
  key(controls,"⌫","Delete",1.3f,this::delete);key(controls,lang==KeyboardLayout.Language.AR?"مسافة":"Space","Space",3f,()->insert(" "));key(controls,"↵","Editor action",1.3f,this::enter);
 }
 private void clearComposition(boolean finish){
  InputConnection old=composingConnection;composingConnection=null;composed="";confirmed=false;composingStart=-1;composingEnd=-1;if(strip!=null)strip.removeAllViews();
  if(finish&&old!=null&&old==getCurrentInputConnection())old.finishComposingText();
 }
 private void toggleVoice(){
  if(!voiceAllowed())return;
  if(voice.busy()){if(voiceProcessing){cancelVoice();}else voice.stop();return;}
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED||!models.installed(settings.model())||!NativeWhisper.available()){Toast.makeText(this,"Open Zero Keyboard setup to grant microphone and import a verified model",Toast.LENGTH_LONG).show();return;}
  clearComposition(true);InputConnection target=getCurrentInputConnection();if(target==null)return;
  long generation=++editorGeneration;
  voice.start(models,settings.model(),new VoiceSession.Listener(){
   public void state(String state){if(generation!=editorGeneration)return;voiceProcessing=state.startsWith("Processing");if(voiceKey!=null)voiceKey.setText(voiceProcessing?"Cancel":state.startsWith("Recording")?"Stop":state.startsWith("Preparing")?"Stop":"Mic");if(state.startsWith("Voice unavailable")||state.startsWith("No speech"))Toast.makeText(ZeroImeService.this,state,Toast.LENGTH_SHORT).show();}
   public void result(String text){if(generation==editorGeneration&&voiceAllowed()&&target==getCurrentInputConnection())target.commitText(text,1);}
  });
 }
 private void glide(List<GeometricRanker.Point> trace,Map<Integer,GeometricRanker.Point> centers){
  cancelVoice();if(!glideAllowed())return;InputConnection c=getCurrentInputConnection();if(c==null)return;
  List<GeometricRanker.Candidate> candidates=ranker.rank(trace,centers,SeedLexicon.words(lang==KeyboardLayout.Language.AR),3);
  if(candidates.isEmpty()||candidates.get(0).score>2.0)return;
  clearComposition(true);if(c!=getCurrentInputConnection())return;
  String word=candidates.get(0).word+(settings.space()?" ":"");
  composingConnection=c;composed=word;confirmed=false;
  if(!c.setComposingText(word,1)){clearComposition(false);return;}
  for(GeometricRanker.Candidate option:candidates)key(strip,option.word,"Glide candidate "+option.word,1f,()->choose(option.word));
 }
 @Override public void onUpdateSelection(int oldSelStart,int oldSelEnd,int newSelStart,int newSelEnd,int candidatesStart,int candidatesEnd){
  super.onUpdateSelection(oldSelStart,oldSelEnd,newSelStart,newSelEnd,candidatesStart,candidatesEnd);
  if(voice.busy()&&(oldSelStart!=newSelStart||oldSelEnd!=newSelEnd))cancelVoice();
  if(composingConnection==null)return;
  if(composingConnection!=getCurrentInputConnection()||!glideAllowed()){clearComposition(false);return;}
  if(candidatesStart>=0&&candidatesEnd-candidatesStart==composed.length()&&newSelStart==candidatesEnd&&newSelEnd==candidatesEnd&&(!confirmed||candidatesStart==composingStart)){
   confirmed=true;composingStart=candidatesStart;composingEnd=candidatesEnd;
  }else clearComposition(true);
 }
 private void choose(String word){
  InputConnection c=getCurrentInputConnection();
  if(c==null||c!=composingConnection||!confirmed||!glideAllowed()){clearComposition(true);return;}
  // Replace only the framework-confirmed composing span, never surrounding text.
  String replacement=word+(settings.space()?" ":"");
  composingConnection=null;confirmed=false;composed="";if(strip!=null)strip.removeAllViews();
  c.commitText(replacement,1);
 }
 private void insert(String value){
  cancelVoice();clearComposition(true);InputConnection c=getCurrentInputConnection();if(c==null)return;c.commitText(value,1);
  if(shift&&!symbols){shift=false;render();}
 }
 private void delete(){
  cancelVoice();clearComposition(true);InputConnection c=getCurrentInputConnection();if(c==null)return;
  if(c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DEL)))c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DEL));else c.deleteSurroundingTextInCodePoints(1,0);
 }
 private void enter(){
  cancelVoice();clearComposition(true);InputConnection c=getCurrentInputConnection();EditorInfo e=getCurrentInputEditorInfo();if(c==null||e==null)return;
  if((e.imeOptions&EditorInfo.IME_FLAG_NO_ENTER_ACTION)==0){if(e.actionLabel!=null&&e.actionId!=0&&c.performEditorAction(e.actionId))return;int a=e.imeOptions&EditorInfo.IME_MASK_ACTION;if(a!=EditorInfo.IME_ACTION_NONE&&a!=EditorInfo.IME_ACTION_UNSPECIFIED&&c.performEditorAction(a))return;}
  if((e.inputType&android.text.InputType.TYPE_MASK_CLASS)==android.text.InputType.TYPE_CLASS_TEXT&&(e.inputType&android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE)!=0)c.commitText("\n",1);
  else {c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));}
 }
}
