package com.zerokeyboard.ime;
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

public final class ZeroImeService extends InlineImeService {
 private KeyboardLayout.Language lang=KeyboardLayout.Language.EN;
 private boolean shift=false,symbols=false,active=false;
 private GlideSurface root;
 private LinearLayout strip,reviewRow;
 private LayoutProfile profile;
 private String pendingVoice="";
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
  root.setPadding(dp(3),dp(4),dp(3),dp(5));root.setBackgroundColor(Color.rgb(24,29,38));render();return wrapWithInlineStrip(root);
 }
 @Override public boolean onEvaluateFullscreenMode(){return false;}
 @Override public void onStartInput(EditorInfo info,boolean restarting){invalidateInlineSession();cancelVoice();clearComposition(false);super.onStartInput(info,restarting);active=true;policy=EditorPolicy.from(info);shift=false;symbols=false;render();}
 @Override public void onStartInputView(EditorInfo info,boolean restarting){invalidateInlineSession();cancelVoice();super.onStartInputView(info,restarting);active=true;policy=EditorPolicy.from(info);render();}
 @Override public void onFinishInputView(boolean finishingInput){invalidateInlineSession();cancelVoice();clearComposition(true);active=false;if(root!=null)root.reset();super.onFinishInputView(finishingInput);}
 @Override public void onFinishInput(){invalidateInlineSession();cancelVoice();clearComposition(false);active=false;policy=EditorPolicy.from(null);shift=false;symbols=false;if(root!=null)root.reset();super.onFinishInput();}
 @Override public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype){
  super.onCurrentInputMethodSubtypeChanged(subtype);if(subtype!=null){cancelVoice();clearComposition(true);lang=subtype.getLocale().startsWith("ar")?KeyboardLayout.Language.AR:KeyboardLayout.Language.EN;shift=false;symbols=false;render();}
 }
 @Override public void onConfigurationChanged(Configuration config){cancelVoice();clearComposition(true);super.onConfigurationChanged(config);render();}
 @Override public void onWindowHidden(){invalidateInlineSession();cancelVoice();super.onWindowHidden();}
 @Override public void onDestroy(){voice.close();super.onDestroy();}
 private void cancelVoice(){pendingVoice="";if(reviewRow!=null){reviewRow.removeAllViews();reviewRow.setVisibility(View.GONE);}editorGeneration++;voice.cancel();voiceProcessing=false;if(voiceKey!=null){voiceKey.setText(R.string.mic_label);voiceKey.setContentDescription(getString(R.string.offline_voice));}}
 private boolean voiceAllowed(){return active&&isInputViewShown()&&settings!=null&&settings.voice()&&!settings.incognito()&&policy.allowPersonalization&&!policy.sensitive;}
 private boolean glideAllowed(){return active&&settings!=null&&settings.glide(lang==KeyboardLayout.Language.AR)&&!settings.incognito()&&policy.allowPersonalization&&!shift&&!symbols&&!policy.numeric;}
 private LayoutProfile profile(){Configuration c=getResources().getConfiguration();boolean tablet=c.smallestScreenWidthDp>=600,landscape=c.orientation==Configuration.ORIENTATION_LANDSCAPE;return LayoutProfile.resolve(c.screenWidthDp,landscape,tablet,settings.layout(tablet,landscape),settings.height(tablet,landscape));}
 private LinearLayout row(){LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);if(profile!=null){if(profile.mode==LayoutProfile.Mode.LEFT)params.rightMargin=dp(profile.sideInset);if(profile.mode==LayoutProfile.Mode.RIGHT)params.leftMargin=dp(profile.sideInset);}root.addView(line,params);return line;}
 private Button key(LinearLayout row,String label,String desc,float weight,Runnable action){
  Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setContentDescription(desc);b.setTextColor(Color.WHITE);b.setTextSize(18);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(0,0,0,0);
  GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(49,58,73));bg.setCornerRadius(dp(8));b.setBackground(bg);
  int h=profile==null?48:profile.keyHeight;
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(h),weight);lp.setMargins(dp(2),dp(2),dp(2),dp(2));row.addView(b,lp);b.setOnClickListener(v->action.run());return b;
 }
 @android.annotation.SuppressLint("ClickableViewAccessibility") private void render(){
  if(root==null)return;profile=profile();root.configure(glideAllowed(),settings.trail());root.removeAllViews();strip=row();strip.setVisibility(glideAllowed()?View.VISIBLE:View.GONE);strip.setMinimumHeight(dp(profile.keyHeight));reviewRow=row();reviewRow.setVisibility(View.GONE);
  if(settings.editorTools()&&active&&!policy.sensitive&&!settings.incognito())editorToolbar();
  if(settings.numberRow()&&!policy.numeric&&!symbols){LinearLayout digits=row();for(String digit:LayoutProfile.digits(settings.indic()&&lang==KeyboardLayout.Language.AR).split(" "))key(digits,digit,digit,1f,()->insert(digit));}
  for(String letters:KeyboardLayout.rows(lang,shift,symbols,policy.numeric)){
   LinearLayout line=row();String[] tokens=letters.split(" ");for(int i=0;i<tokens.length;i++){if(profile.gap>0&&!policy.numeric&&i==tokens.length/2){View gap=new View(this);line.addView(gap,new LinearLayout.LayoutParams(dp(profile.gap),1));}String text=policy.numeric?LayoutProfile.localizeDigits(tokens[i],settings.indic()&&lang==KeyboardLayout.Language.AR):tokens[i];Button b=key(line,text,text,1f,()->insert(text));root.register(b,text);}
  }
  LinearLayout controls=row();
  if(!policy.numeric){
   key(controls,lang==KeyboardLayout.Language.AR?"EN":"ع","Switch language",1.3f,()->{cancelVoice();clearComposition(true);lang=lang==KeyboardLayout.Language.EN?KeyboardLayout.Language.AR:KeyboardLayout.Language.EN;shift=false;symbols=false;render();});
   key(controls,symbols?"ABC":"123","Symbols",1.3f,()->{cancelVoice();clearComposition(true);symbols=!symbols;shift=false;render();});
   key(controls,"⇧","Shift or diacritics",1f,()->{cancelVoice();clearComposition(true);shift=!shift;render();});
  }
  voiceKey=null;if(active&&settings.voice()&&!settings.incognito()&&policy.allowPersonalization&&!policy.sensitive)voiceKey=key(controls,"Mic","Offline voice typing",1.5f,this::toggleVoice);
  key(controls,"⌫","Delete",1.3f,this::delete);Button space=key(controls,lang==KeyboardLayout.Language.AR?"مسافة":"Space","Space",3f,()->insert(" "));if(!policy.sensitive&&settings.spaceCursor())space.setOnTouchListener(new SpaceCursor());key(controls,"↵","Editor action",1.3f,this::enter);
 }
 private void editorToolbar(){
  LinearLayout tools=row();
  key(tools,"←","Cursor left",1f,()->moveCursor(KeyEvent.KEYCODE_DPAD_LEFT));
  key(tools,"→","Cursor right",1f,()->moveCursor(KeyEvent.KEYCODE_DPAD_RIGHT));
  key(tools,"All","Select all",1f,()->editorAction(android.R.id.selectAll));
  key(tools,"Copy","Copy selected",1f,()->editorAction(android.R.id.copy));
  key(tools,"Cut","Cut selected",1f,()->editorAction(android.R.id.cut));
  key(tools,"Paste","Paste",1f,()->editorAction(android.R.id.paste));
 }
 private void editorAction(int action){
  if(!active||policy.sensitive||settings.incognito())return;cancelVoice();clearComposition(true);InputConnection c=getCurrentInputConnection();if(c!=null)c.performContextMenuAction(action);
 }
 private void moveCursor(int code){
  cancelVoice();clearComposition(true);InputConnection c=getCurrentInputConnection();if(c!=null){c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));}
 }
 private final class SpaceCursor implements View.OnTouchListener{
  private float origin,last;private boolean dragging;
  public boolean onTouch(View v,MotionEvent e){
   switch(e.getActionMasked()){
    case MotionEvent.ACTION_DOWN:origin=last=e.getX();dragging=false;return true;
    case MotionEvent.ACTION_MOVE:
     float x=e.getX();if(Math.abs(x-origin)>dp(12))dragging=true;
     if(dragging){int steps=(int)((x-last)/dp(14));steps=Math.max(-8,Math.min(8,steps));for(int i=0;i<Math.abs(steps);i++)moveCursor(steps<0?KeyEvent.KEYCODE_DPAD_LEFT:KeyEvent.KEYCODE_DPAD_RIGHT);last+=steps*dp(14);}return true;
    case MotionEvent.ACTION_UP:if(!dragging&&e.getX()>=0&&e.getX()<v.getWidth()&&e.getY()>=0&&e.getY()<v.getHeight())v.performClick();return true;
    case MotionEvent.ACTION_CANCEL:case MotionEvent.ACTION_POINTER_DOWN:dragging=true;return true;
    default:return true;
   }
  }
 }
 private void reviewVoice(String text,long generation,InputConnection target){
  if(reviewRow==null)return;pendingVoice=text;reviewRow.removeAllViews();reviewRow.setVisibility(View.VISIBLE);
  TextView preview=new TextView(this);preview.setText(text);preview.setTextColor(Color.WHITE);preview.setMaxLines(2);preview.setEllipsize(android.text.TextUtils.TruncateAt.END);reviewRow.addView(preview,new LinearLayout.LayoutParams(0,-2,3f));
  key(reviewRow,"Insert","Insert dictated text",1f,()->{String value=pendingVoice;boolean valid=generation==editorGeneration&&voiceAllowed()&&target==getCurrentInputConnection();cancelVoice();if(valid&&!value.isEmpty())target.commitText(value,1);});
  key(reviewRow,"×","Discard dictated text",.7f,this::cancelVoice);
 }
 private void clearComposition(boolean finish){
  InputConnection old=composingConnection;composingConnection=null;composed="";confirmed=false;composingStart=-1;composingEnd=-1;if(strip!=null)strip.removeAllViews();
  if(finish&&old!=null&&old==getCurrentInputConnection())old.finishComposingText();
 }
 private void toggleVoice(){
  if(!voiceAllowed())return;
  if(voice.busy()){if(voiceProcessing){cancelVoice();}else voice.stop();return;}
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED||!models.installed(settings.model())||!NativeWhisper.available()){Toast.makeText(this,"Open Zero Keyboard setup to grant microphone and import a verified model",Toast.LENGTH_LONG).show();return;}
  cancelVoice();clearComposition(true);InputConnection target=getCurrentInputConnection();if(target==null)return;
  long generation=++editorGeneration;
  voice.start(this,models,settings.model(),settings.stopOnSilence(),new VoiceSession.Listener(){
   public void state(String state){if(generation!=editorGeneration)return;voiceProcessing=state.startsWith("Processing");if(voiceKey!=null)voiceKey.setText(voiceProcessing?"Cancel":state.startsWith("Recording")?"REC • Stop":state.startsWith("Preparing")?"Stop":"Mic");if(voiceKey!=null)voiceKey.setContentDescription(state);if(state.startsWith("Voice unavailable")||state.startsWith("No speech"))Toast.makeText(ZeroImeService.this,state,Toast.LENGTH_SHORT).show();}
   public void result(String text){if(generation==editorGeneration&&voiceAllowed()&&target==getCurrentInputConnection()){if(settings.voiceReview())reviewVoice(text,generation,target);else target.commitText(text,1);}}
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
  if((voice.busy()||!pendingVoice.isEmpty())&&(oldSelStart!=newSelStart||oldSelEnd!=newSelEnd))cancelVoice();
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
