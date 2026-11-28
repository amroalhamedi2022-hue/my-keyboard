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
import com.zerokeyboard.ime.glide.FrequencyLexicon;
import com.zerokeyboard.ime.glide.DictionaryResourceStore;
import java.util.concurrent.*;
import com.zerokeyboard.ime.tools.ClipboardHistory;
import com.zerokeyboard.ime.tools.EmojiCatalog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.os.SystemClock;
import android.Manifest;
import android.content.pm.PackageManager;
import com.zerokeyboard.ime.voice.*;

public final class ZeroImeService extends InlineImeService {
 private KeyboardLayout.Language lang=KeyboardLayout.Language.EN;
 private boolean shift=false,capsLock=false,symbols=false,active=false,toolsExpanded=false;
 private int mainHeight=48,barHeight=36;
 private android.widget.PopupWindow alternatePopup;
 private final android.os.Handler deleteTimer=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable deleteRepeat=this::repeatDelete;
 private Button heldDelete;
 private GlideSurface root;
 private LinearLayout strip,reviewRow;
 private final ClipboardHistory clipboard=new ClipboardHistory();
 private final android.os.Handler clipboardTimer=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable clipboardExpiry=this::expireClipboard;
 private int panel,emojiGroup;
 private boolean emojiSearchMode,emojiQueryArabic;
 private String emojiQuery="";
 private TextView emojiQueryLabel;
 private LinearLayout emojiMatches;
 private ThemeTokens style=ThemeTokens.resolve(1,true,8,2,18,4);
 private final ArrayList<String> recentEmoji=new ArrayList<>();
 private LayoutProfile profile;
 private String pendingVoice="",partialVoice="";
 private TextView liveVoicePreview;
 private EditorPolicy policy=EditorPolicy.from(null);
 private ZeroSettings settings;
 private final VoiceSession voice=new VoiceSession();
 private ModelStore models;
 private Button voiceKey;
 private boolean buildingBar;
 private long editorGeneration;
 private boolean voiceProcessing;
 private final GeometricRanker ranker=new GeometricRanker();
 private DictionaryResourceStore dictionaries;
 private final android.os.Handler glideMain=new android.os.Handler(android.os.Looper.getMainLooper());
 private final ThreadPoolExecutor glideWorker=new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1),new ThreadPoolExecutor.DiscardOldestPolicy());
 private volatile long glideGeneration;
 private boolean glidePending;
 private InputConnection composingConnection;
 private String composed="";
 private boolean confirmed=false;
 private int composingStart=-1,composingEnd=-1;
 private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
 @Override public View onCreateInputView(){
  settings=new ZeroSettings(this);models=new ModelStore(this);dictionaries=new DictionaryResourceStore(this);root=new GlideSurface(this,this::glide);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
  root.setPadding(dp(5),dp(2),dp(5),dp(4));root.setBackgroundColor(Color.rgb(24,29,38));render();return wrapWithInlineStrip(root);
 }
 @Override public boolean onEvaluateFullscreenMode(){return false;}
 @Override public void onStartInput(EditorInfo info,boolean restarting){invalidateInlineSession();cancelVoice();clearComposition(false);super.onStartInput(info,restarting);active=true;policy=EditorPolicy.from(info);resetPanels();shift=false;capsLock=false;symbols=false;render();}
 @Override public void onStartInputView(EditorInfo info,boolean restarting){invalidateInlineSession();cancelVoice();super.onStartInputView(info,restarting);active=true;policy=EditorPolicy.from(info);resetPanels();render();}
 @Override public void onFinishInputView(boolean finishingInput){stopDeleteRepeat();invalidateInlineSession();cancelVoice();clearComposition(true);active=false;resetPanels();if(root!=null)root.reset();super.onFinishInputView(finishingInput);}
 @Override public void onFinishInput(){stopDeleteRepeat();invalidateInlineSession();cancelVoice();clearComposition(false);active=false;clearTransientPanels();policy=EditorPolicy.from(null);shift=false;capsLock=false;symbols=false;if(root!=null)root.reset();super.onFinishInput();}
 @Override public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype){
  super.onCurrentInputMethodSubtypeChanged(subtype);if(subtype!=null){cancelVoice();clearComposition(true);lang=subtype.getLocale().startsWith("ar")?KeyboardLayout.Language.AR:KeyboardLayout.Language.EN;shift=false;capsLock=false;symbols=false;render();}
 }
 @Override public void onConfigurationChanged(Configuration config){cancelVoice();clearComposition(true);super.onConfigurationChanged(config);render();}
 @Override public void onWindowHidden(){stopDeleteRepeat();invalidateInlineSession();cancelVoice();clearTransientPanels();super.onWindowHidden();}
 @Override public void onDestroy(){stopDeleteRepeat();cancelGlide();glideWorker.shutdownNow();clearTransientPanels();recentEmoji.clear();voice.close();super.onDestroy();}
 private void expireClipboard(){if(settings!=null){clipboard.prune(SystemClock.elapsedRealtime(),settings.clipboardTtl());if(panel==1&&isInputViewShown())render();else scheduleClipboardExpiry();}}
 private void scheduleClipboardExpiry(){clipboardTimer.removeCallbacks(clipboardExpiry);if(!toolsAllowed()||!settings.clipboard())return;long delay=clipboard.nextExpiry(SystemClock.elapsedRealtime(),settings.clipboardTtl());if(delay>=0)clipboardTimer.postDelayed(clipboardExpiry,delay);}
 private void cancelGlide(){glideGeneration++;glideWorker.getQueue().clear();boolean pending=glidePending;glidePending=false;if(pending)restoreBar();}
 private void cancelVoice(){cancelGlide();pendingVoice="";partialVoice="";liveVoicePreview=null;if(reviewRow!=null){reviewRow.removeAllViews();reviewRow.setVisibility(View.GONE);}editorGeneration++;voice.cancel();voiceProcessing=false;if(voiceKey!=null){voiceKey.setText("🎙");voiceKey.setContentDescription(getString(R.string.offline_voice));}if(strip!=null&&strip.getChildCount()==0)restoreBar();}
 private boolean voiceAllowed(){return active&&isInputViewShown()&&settings!=null&&settings.voice()&&!settings.incognito()&&policy.allowPersonalization&&!policy.sensitive;}
 private boolean glideAllowed(){return panel==0&&active&&settings!=null&&settings.glide(lang==KeyboardLayout.Language.AR)&&!settings.incognito()&&policy.allowPersonalization&&!shift&&!symbols&&!policy.numeric;}
 private LayoutProfile profile(){Configuration c=getResources().getConfiguration();boolean tablet=c.smallestScreenWidthDp>=600,landscape=c.orientation==Configuration.ORIENTATION_LANDSCAPE;return LayoutProfile.resolve(c.screenWidthDp,landscape,tablet,settings.layout(tablet,landscape),settings.height(tablet,landscape));}
 private LinearLayout row(){LinearLayout line=new KeyRow(this);line.setGravity(Gravity.CENTER);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);if(profile!=null){if(profile.mode==LayoutProfile.Mode.LEFT)params.rightMargin=dp(profile.sideInset);if(profile.mode==LayoutProfile.Mode.RIGHT)params.leftMargin=dp(profile.sideInset);}root.addView(line,params);return line;}
 private Button key(LinearLayout row,String label,String desc,float weight,Runnable action){
  Button b=new KeyButton(this);b.setAllCaps(false);b.setText(label);b.setContentDescription(desc);b.setTextColor(style.text);b.setTextSize(style.fontSize);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(0,0,0,0);b.setGravity(Gravity.CENTER);b.setIncludeFontPadding(false);b.setStateListAnimator(null);b.setSingleLine(true);b.setEllipsize(android.text.TextUtils.TruncateAt.END);if(buildingBar)b.setTextSize(16);
  GradientDrawable bg=new GradientDrawable();bg.setColor(buildingBar?style.modifier:style.key);bg.setCornerRadius(dp(style.radius));if(!buildingBar)bg.setStroke(dp(1),style.stroke);b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(style.press),bg,null));b.setSoundEffectsEnabled(settings.keySound());b.setHapticFeedbackEnabled(settings.keyHaptic());
  int h=buildingBar?barHeight:mainHeight;
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(h),weight);int gap=buildingBar?2:Math.min(3,style.spacing);lp.setMargins(dp(gap),dp(gap),dp(gap),dp(gap));row.addView(b,lp);b.setOnClickListener(v->{if(settings.keyHaptic())v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);action.run();});return b;
 }
 @android.annotation.SuppressLint("ClickableViewAccessibility") private void render(){
  if(root==null)return;stopDeleteRepeat();cancelGlide();clipboardTimer.removeCallbacks(clipboardExpiry);profile=profile();style=settings.theme((getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);root.setBackgroundColor(style.background);root.trailStyle(style.accent,style.trailWidth);if(!toolsAllowed()){panel=0;clipboard.clear();}if(!settings.clipboard()){clipboard.clear();if(panel==1)panel=0;}if(!settings.emoji()&&panel==2)panel=0;scheduleClipboardExpiry();root.configure(glideAllowed(),settings.trail());root.removeAllViews();Configuration cfg=getResources().getConfiguration();barHeight=CompactMetrics.toolbar(profile.keyHeight);mainHeight=CompactMetrics.keyHeight(cfg.screenHeightDp,profile.keyHeight,cfg.orientation==Configuration.ORIENTATION_LANDSCAPE,4,barHeight,settings.numberRow()?barHeight:0);strip=row();reviewRow=row();reviewRow.setVisibility(View.GONE);restoreBar();
  if(panel!=0){strip.setVisibility(View.GONE);renderPanel();return;}
  if(settings.numberRow()&&!policy.numeric&&!symbols){LinearLayout digits=row();buildingBar=true;for(String digit:LayoutProfile.digits(settings.indic()&&lang==KeyboardLayout.Language.AR).split(" "))key(digits,digit,digit,1f,()->insert(digit));buildingBar=false;}
  int rowIndex=0;for(String letters:KeyboardLayout.rows(lang,shift,symbols,policy.numeric)){
   LinearLayout line=row();String[] tokens=letters.split(" ");boolean latin=lang==KeyboardLayout.Language.EN&&!symbols&&!policy.numeric;
   if(latin&&rowIndex==1)addWeightSpace(line,.5f);
   if(latin&&rowIndex==2)modifier(line,shiftButton(line));for(int i=0;i<tokens.length;i++){if(profile.gap>0&&!policy.numeric&&i==tokens.length/2){View gap=new View(this);line.addView(gap,new LinearLayout.LayoutParams(dp(profile.gap),1));}String text=policy.numeric?LayoutProfile.localizeDigits(tokens[i],settings.indic()&&lang==KeyboardLayout.Language.AR):tokens[i];Button b=key(line,text,text,1f,()->insert(text));root.register(b,text);if(!policy.numeric&&!symbols&&KeyboardLayout.alternatives(text).length>0)b.setOnLongClickListener(v->showAlternatives(b,text));}
   if(latin&&rowIndex==1)addWeightSpace(line,.5f);if(latin&&rowIndex==2)modifier(line,deleteButton(line));rowIndex++;
  }
  LinearLayout controls=row();
  if(!policy.numeric){
   modifier(controls,key(controls,lang==KeyboardLayout.Language.AR?"EN":"ع","Switch language",1.1f,()->{cancelVoice();clearComposition(true);lang=lang==KeyboardLayout.Language.EN?KeyboardLayout.Language.AR:KeyboardLayout.Language.EN;shift=false;capsLock=false;symbols=false;render();}));
   modifier(controls,key(controls,symbols?"ABC":"123","Symbols",1.3f,()->{cancelVoice();clearComposition(true);symbols=!symbols;shift=false;capsLock=false;render();}));
   if(lang==KeyboardLayout.Language.AR&&!symbols)modifier(controls,shiftButton(controls));
   Button comma=key(controls,lang==KeyboardLayout.Language.AR?"،":",","Comma",.65f,()->insert(lang==KeyboardLayout.Language.AR?"،":","));comma.setOnLongClickListener(v->showAlternatives(comma,comma.getText().toString()));
  }
  if(policy.numeric||symbols||lang==KeyboardLayout.Language.AR)modifier(controls,deleteButton(controls));
  Button space=key(controls,lang==KeyboardLayout.Language.AR?"العربية":"English","Space",4f,()->insert(" "));if(!policy.sensitive&&settings.spaceCursor())space.setOnTouchListener(new SpaceCursor());if(!policy.numeric){Button dot=key(controls,".","Period",.65f,()->insert("."));dot.setOnLongClickListener(v->showAlternatives(dot,"."));}action(key(controls,"↵","Editor action",1.2f,this::enter));
 }
 private void editorToolbar(LinearLayout tools){
  key(tools,"←","Cursor left",1f,()->moveCursor(KeyEvent.KEYCODE_DPAD_LEFT));
  key(tools,"→","Cursor right",1f,()->moveCursor(KeyEvent.KEYCODE_DPAD_RIGHT));
  key(tools,"All","Select all",1f,()->editorAction(android.R.id.selectAll));
  key(tools,"Copy","Copy selected",1f,()->editorAction(android.R.id.copy));
  key(tools,"Cut","Cut selected",1f,()->editorAction(android.R.id.cut));
  key(tools,"Paste","Paste",1f,()->editorAction(android.R.id.paste));

 }
 private void addWeightSpace(LinearLayout row,float weight){row.addView(new View(this),new LinearLayout.LayoutParams(0,1,weight));}
 private void decorate(Button b,int color,int text){GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(style.radius));b.setTextColor(text);b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(style.press),bg,null));}
 private void modifier(LinearLayout row,Button b){decorate(b,style.modifier,style.text);b.setTextSize(Math.min(20,style.fontSize));}
 private void action(Button b){decorate(b,style.action,style.actionText);}
 private Button shiftButton(LinearLayout row){Button b=key(row,capsLock?"⇪":"⇧",capsLock?"Caps lock":"Shift or diacritics",1.25f,()->{cancelVoice();clearComposition(true);if(capsLock){capsLock=false;shift=false;}else shift=!shift;render();});if(lang==KeyboardLayout.Language.EN&&!symbols)b.setOnLongClickListener(v->{cancelVoice();clearComposition(true);capsLock=!capsLock;shift=capsLock;render();return true;});return b;}
 private Button deleteButton(LinearLayout row){Button b=key(row,"⌫","Delete",1.25f,this::delete);b.setOnLongClickListener(v->{if(!active)return true;heldDelete=b;delete();deleteTimer.postDelayed(deleteRepeat,90);return true;});b.setOnTouchListener((v,e)->{int a=e.getActionMasked();if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL||a==MotionEvent.ACTION_POINTER_DOWN)stopDeleteRepeat();else if(a==MotionEvent.ACTION_MOVE&&(e.getX()<0||e.getX()>=v.getWidth()||e.getY()<0||e.getY()>=v.getHeight()))stopDeleteRepeat();return false;});return b;}
 private boolean showAlternatives(Button anchor,String value){String[] choices=KeyboardLayout.alternatives(value);if(choices.length==0||!active)return false;if(choices.length==1){insert(choices[0]);return true;}if(alternatePopup!=null)alternatePopup.dismiss();LinearLayout row=new LinearLayout(this);row.setPadding(dp(4),dp(4),dp(4),dp(4));GradientDrawable bg=new GradientDrawable();bg.setColor(style.background);bg.setCornerRadius(dp(12));row.setBackground(bg);for(String choice:choices){Button b=new Button(this);b.setAllCaps(false);b.setText(choice);b.setTextSize(20);b.setTextColor(style.text);decorate(b,style.key,style.text);row.addView(b,new LinearLayout.LayoutParams(dp(38),dp(46)));b.setOnClickListener(v->{if(alternatePopup!=null)alternatePopup.dismiss();alternatePopup=null;insert(choice);});}alternatePopup=new PopupWindow(row,Math.min(getResources().getDisplayMetrics().widthPixels,dp(8+38*choices.length)),dp(54),false);alternatePopup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));alternatePopup.setOutsideTouchable(true);alternatePopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);alternatePopup.showAsDropDown(anchor,0,-anchor.getHeight()-dp(54));return true;}
 private void openSettings(){openSettings(false);}
 private void openSettings(boolean voiceSection){cancelVoice();clearComposition(true);android.content.Intent i=new android.content.Intent(this,SetupActivity.class);if(voiceSection)i.putExtra("section","voice");i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}
 private void restoreBar(){
  if(strip==null||settings==null||glidePending||composingConnection!=null||voice.busy()||!pendingVoice.isEmpty())return;
  strip.removeAllViews();voiceKey=null;
  boolean allowed=toolsAllowed()&&panel==0&&!policy.numeric;strip.setVisibility(allowed?View.VISIBLE:View.GONE);if(!allowed)return;
  buildingBar=true;try{
   if(settings.editorTools())key(strip,toolsExpanded?"‹":"⋯","Editor tools",.8f,()->{toolsExpanded=!toolsExpanded;restoreBar();});
   if(toolsExpanded&&settings.editorTools()){editorToolbar(strip);return;}
   if(settings.emoji())key(strip,"☺","Emoji panel",1f,()->openPanel(2));
   if(settings.clipboard())key(strip,"▣","Session clipboard",1f,()->openPanel(1));
   TextView mode=new TextView(this);mode.setText(lang==KeyboardLayout.Language.AR?"العربية":"English");mode.setTextColor(style.muted);mode.setGravity(Gravity.CENTER);strip.addView(mode,new LinearLayout.LayoutParams(0,dp(barHeight),2f));
   voiceKey=key(strip,"🎙","Offline voice typing",1f,()->{if(settings.voice()&&models.installed(settings.model())&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)toggleVoice();else openSettings(true);});
   key(strip,"⚙","Keyboard settings",1f,this::openSettings);
  }finally{buildingBar=false;}
 }
 private boolean toolsAllowed(){return active&&settings!=null&&policy.allowPersonalization&&!policy.sensitive&&!settings.incognito();}
 private void clearTransientPanels(){emojiSearchMode=false;emojiQuery="";emojiQueryLabel=null;emojiMatches=null;clipboardTimer.removeCallbacks(clipboardExpiry);clipboard.clear();if(panel!=0&&root!=null){root.reset();root.removeAllViews();}panel=0;}
 private void resetPanels(){toolsExpanded=false;if(!toolsAllowed()||!settings.clipboard())clearTransientPanels();else panel=0;}
 private void openPanel(int value){if(!toolsAllowed())return;cancelVoice();clearComposition(true);emojiSearchMode=false;emojiQuery="";emojiQueryArabic=lang==KeyboardLayout.Language.AR;panel=value;render();}
 private void renderPanel(){
  LinearLayout controls=row();key(controls,"ABC","Back to keyboard",1f,()->{panel=0;emojiSearchMode=false;emojiQuery="";emojiQueryLabel=null;emojiMatches=null;render();});
  if(panel==1){
   key(controls,"Save","Save current clipboard",1.7f,this::saveClipboard);
   key(controls,"Clear","Clear history",1f,()->{clipboard.clear();render();});
   long now=SystemClock.elapsedRealtime(),ttl=settings.clipboardTtl();
   List<ClipboardHistory.Entry> entries=clipboard.list(now,ttl);
   if(entries.isEmpty()){TextView empty=new TextView(this);empty.setText("No saved text");empty.setTextColor(style.text);root.addView(empty);}
   // Cap visible rows to avoid covering the editor in landscape.
   ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,dp(profile.keyHeight*3)));
   for(ClipboardHistory.Entry e:entries){LinearLayout line=new LinearLayout(this);list.addView(line);
    String label=e.text.replace('\n',' ');if(label.length()>24)label=label.substring(0,24)+"…";
    key(line,label,"Paste saved text",3f,()->pasteSaved(e.id));key(line,e.pinned?"Unpin":"Pin","Toggle pin",1f,()->{clipboard.pin(e.id,!e.pinned,SystemClock.elapsedRealtime(),settings.clipboardTtl());render();});key(line,"×","Remove entry",.7f,()->{clipboard.remove(e.id);render();});
   }
  }else{
   key(controls,emojiSearchMode?"Categories":"Search","Search emoji",1f,()->{emojiSearchMode=!emojiSearchMode;emojiQuery="";render();});
   if(emojiSearchMode){renderEmojiSearch();return;}
   String[] names={"Recent","Faces","Gestures","Nature","Objects"};
   LinearLayout tabs=row();for(int i=0;i<names.length;i++){final int selected=i-1;key(tabs,names[i],"Emoji "+names[i],1f,()->{emojiGroup=selected;render();});}
   String[] emojis=emojiGroup<0?recentEmoji.toArray(new String[0]):EmojiCatalog.group(emojiGroup);
   for(int i=0;i<emojis.length;i+=8){LinearLayout line=row();for(int j=i;j<Math.min(i+8,emojis.length);j++){String emoji=emojiGroup<0?emojis[j]:EmojiCatalog.skinTone(emojis[j],settings.emojiTone());key(line,emoji,"Insert emoji "+emoji,1f,()->insertEmoji(emoji));}}
  }
 }
 private void insertEmoji(String emoji){if(!toolsAllowed()||!settings.emoji())return;recentEmoji.remove(emoji);recentEmoji.add(0,emoji);if(recentEmoji.size()>24)recentEmoji.remove(24);insert(emoji);}
 private void renderEmojiSearch(){
  emojiQueryLabel=new TextView(this);emojiQueryLabel.setTextColor(style.text);emojiQueryLabel.setTextSize(style.fontSize);root.addView(emojiQueryLabel);
  emojiMatches=new LinearLayout(this);emojiMatches.setOrientation(LinearLayout.VERTICAL);root.addView(emojiMatches,new LinearLayout.LayoutParams(-1,-2));
  for(String letters:KeyboardLayout.rows(emojiQueryArabic?KeyboardLayout.Language.AR:KeyboardLayout.Language.EN,false,false,false)){LinearLayout line=row();for(String letter:letters.split(" "))key(line,letter,"Emoji search "+letter,1f,()->{if(emojiQuery.length()+letter.length()<=48){emojiQuery+=letter;updateEmojiMatches();}});}
  LinearLayout controls=row();key(controls,emojiQueryArabic?"EN":"ع","Switch emoji search language",1f,()->{emojiQueryArabic=!emojiQueryArabic;render();});
  key(controls,"Space","Emoji search space",2f,()->{if(emojiQuery.length()<48){emojiQuery+=" ";updateEmojiMatches();}});
  key(controls,"⌫","Delete search character",1f,()->{if(!emojiQuery.isEmpty()){emojiQuery=emojiQuery.substring(0,emojiQuery.offsetByCodePoints(emojiQuery.length(),-1));updateEmojiMatches();}});
  key(controls,"Clear","Clear emoji search",1f,()->{emojiQuery="";updateEmojiMatches();});updateEmojiMatches();
 }
 private void updateEmojiMatches(){
  if(emojiMatches==null||emojiQueryLabel==null)return;emojiQueryLabel.setText(emojiQuery.isEmpty()?"Search emoji / ابحث عن رمز":emojiQuery);emojiMatches.removeAllViews();
  List<String> matches=EmojiCatalog.search(emojiQuery,16);
  if(matches.isEmpty()){TextView label=new TextView(this);label.setTextColor(style.text);label.setText(emojiQuery.trim().isEmpty()?"Type a keyword":"No matches");emojiMatches.addView(label);return;}
  for(int i=0;i<matches.size();i+=8){LinearLayout line=new LinearLayout(this);emojiMatches.addView(line);for(int j=i;j<Math.min(i+8,matches.size());j++){String emoji=EmojiCatalog.skinTone(matches.get(j),settings.emojiTone());key(line,emoji,"Insert emoji "+emoji,1f,()->insertEmoji(emoji));}}
 }
 private void saveClipboard(){
  if(!toolsAllowed()||!settings.clipboard()||!isInputViewShown())return;
  boolean saved=false;
  try{ClipboardManager manager=getSystemService(ClipboardManager.class);ClipData clip=manager==null?null:manager.getPrimaryClip();
   if(clip!=null&&clip.getItemCount()==1&&clip.getDescription().hasMimeType("text/plain")){
    android.os.PersistableBundle extras=clip.getDescription().getExtras();boolean sensitive=extras!=null&&(extras.getBoolean("android.content.extra.IS_SENSITIVE",false)||extras.getBoolean("android.content.extra.IS_SENSITIVE_CLIP",false));
    CharSequence value=clip.getItemAt(0).getText();if(value!=null&&value.length()<=ClipboardHistory.MAX_TEXT)saved=clipboard.save(value.toString(),sensitive,SystemClock.elapsedRealtime(),settings.clipboardTtl());
   }
  }catch(RuntimeException ignored){}
  if(!saved)Toast.makeText(this,"Clipboard unavailable or not eligible for saving",Toast.LENGTH_SHORT).show();render();
 }
 private void pasteSaved(long id){if(!toolsAllowed()||!settings.clipboard())return;for(ClipboardHistory.Entry e:clipboard.list(SystemClock.elapsedRealtime(),settings.clipboardTtl()))if(e.id==id){insert(e.text);panel=0;render();return;}}
 private void editorAction(int action){
  if(!toolsAllowed())return;cancelVoice();clearComposition(true);InputConnection c=getCurrentInputConnection();if(c!=null)c.performContextMenuAction(action);
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
  TextView preview=new TextView(this);preview.setText(text);preview.setTextColor(style.text);preview.setMaxLines(2);preview.setEllipsize(android.text.TextUtils.TruncateAt.END);reviewRow.addView(preview,new LinearLayout.LayoutParams(0,-2,3f));
  key(reviewRow,"Insert","Insert dictated text",1f,()->{String value=pendingVoice;boolean valid=generation==editorGeneration&&voiceAllowed()&&target==getCurrentInputConnection();cancelVoice();if(valid&&!value.isEmpty())target.commitText(value,1);});
  key(reviewRow,"×","Discard dictated text",.7f,this::cancelVoice);
 }
 private void clearComposition(boolean finish){
  InputConnection old=composingConnection;composingConnection=null;composed="";confirmed=false;composingStart=-1;composingEnd=-1;if(old!=null&&strip!=null)strip.removeAllViews();
  if(finish&&old!=null&&old==getCurrentInputConnection())old.finishComposingText();if(old!=null)restoreBar();
 }
 private void toggleVoice(){
  if(!voiceAllowed())return;
  if(voice.busy()){if(voiceProcessing){cancelVoice();}else voice.stop();return;}
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED||!models.installed(settings.model())||!NativeWhisper.available()){Toast.makeText(this,"Open Zero Keyboard setup to grant microphone and import a verified model",Toast.LENGTH_LONG).show();return;}
  cancelVoice();clearComposition(true);InputConnection target=getCurrentInputConnection();if(target==null)return;
  long generation=++editorGeneration;
  voice.start(this,models,settings.model(),settings.voiceOptions(),settings.continuousVoice(),new VoiceSession.Listener(){
   public void state(String state){if(generation!=editorGeneration)return;voiceProcessing=state.startsWith("Processing");boolean paused=state.startsWith("Paused"),recording=state.startsWith("Recording");if(voiceKey!=null){voiceKey.setText(voiceProcessing?"Cancel":recording?"REC • Stop":paused?"Stop":state.startsWith("Preparing")?"Stop":"🎙");voiceKey.setContentDescription(state);}
    if(reviewRow!=null){reviewRow.removeAllViews();liveVoicePreview=null;boolean show=recording||paused||voiceProcessing;reviewRow.setVisibility(show?View.VISIBLE:View.GONE);if(show){liveVoicePreview=new TextView(ZeroImeService.this);liveVoicePreview.setText(partialVoice.isEmpty()?state:partialVoice);liveVoicePreview.setTextColor(style.text);liveVoicePreview.setMaxLines(2);liveVoicePreview.setEllipsize(android.text.TextUtils.TruncateAt.START);reviewRow.addView(liveVoicePreview,new LinearLayout.LayoutParams(0,-2,3f));}if(recording||paused){key(reviewRow,paused?"Resume":"Pause",paused?"Resume recording":"Pause recording and release microphone capture",1f,voice::togglePause);key(reviewRow,"Discard","Cancel voice session",1f,ZeroImeService.this::cancelVoice);}}
    if(!recording&&!paused&&!voiceProcessing)partialVoice="";
    if(state.startsWith("Voice unavailable")||state.startsWith("No speech")||state.contains("backlog"))Toast.makeText(ZeroImeService.this,state,Toast.LENGTH_SHORT).show();}
   public void partial(String text){if(generation==editorGeneration&&voiceAllowed()&&target==getCurrentInputConnection()){partialVoice=text;if(liveVoicePreview!=null)liveVoicePreview.setText(text);}}
   public void result(String text){partialVoice="";liveVoicePreview=null;if(generation==editorGeneration&&voiceAllowed()&&target==getCurrentInputConnection()){if(settings.voiceReview())reviewVoice(text,generation,target);else target.commitText(text,1);}}
  });
 }
 private void glide(List<GeometricRanker.Point> trace,Map<Integer,GeometricRanker.Point> centers){
  cancelVoice();if(!glideAllowed())return;InputConnection c=getCurrentInputConnection();if(c==null)return;
  clearComposition(true);if(c!=getCurrentInputConnection())return;
  boolean ar=lang==KeyboardLayout.Language.AR,imported=settings.importedDictionary();List<String> personal=settings.personalWords(ar);
  DictionaryResourceStore resources=dictionaries;long token=++glideGeneration;glidePending=true;
  TextView status=new TextView(this);status.setText("Recognizing…");status.setTextColor(style.text);if(strip!=null){strip.removeAllViews();strip.setVisibility(View.VISIBLE);strip.addView(status,new LinearLayout.LayoutParams(-1,dp(barHeight)));}
  glideWorker.execute(()->{if(token!=glideGeneration)return;
   List<GeometricRanker.Candidate> candidates;
   try{List<String> words=FrequencyLexicon.merge(SeedLexicon.words(ar),personal,resources.words(ar,imported));candidates=ranker.rank(trace,centers,words,3);}
   catch(RuntimeException e){candidates=Collections.emptyList();}
   List<GeometricRanker.Candidate> result=candidates;
   glideMain.post(()->applyGlide(token,c,result));
  });
 }
 private void applyGlide(long token,InputConnection c,List<GeometricRanker.Candidate> candidates){
  if(token!=glideGeneration)return;glidePending=false;if(strip!=null)strip.removeAllViews();
  if(!glideAllowed()||!isInputViewShown()||c!=getCurrentInputConnection()||candidates.isEmpty()||candidates.get(0).score>2.0){restoreBar();return;}
  String word=candidates.get(0).word+(settings.space()?" ":"");
  composingConnection=c;composed=word;confirmed=false;
  if(!c.setComposingText(word,1)){clearComposition(false);restoreBar();return;}
  buildingBar=true;try{for(GeometricRanker.Candidate option:candidates)key(strip,option.word,"Glide candidate "+option.word,1f,()->choose(option.word));}finally{buildingBar=false;}
 }
 @Override public void onUpdateSelection(int oldSelStart,int oldSelEnd,int newSelStart,int newSelEnd,int candidatesStart,int candidatesEnd){
  super.onUpdateSelection(oldSelStart,oldSelEnd,newSelStart,newSelEnd,candidatesStart,candidatesEnd);
  if(glidePending&&(oldSelStart!=newSelStart||oldSelEnd!=newSelEnd))cancelGlide();
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
  c.commitText(replacement,1);restoreBar();
 }
 private void insert(String value){
  cancelVoice();clearComposition(true);InputConnection c=getCurrentInputConnection();if(c==null)return;c.commitText(value,1);
  if(shift&&!capsLock&&!symbols){shift=false;render();}else if(strip!=null&&strip.getChildCount()==0)restoreBar();
 }
 private void stopDeleteRepeat(){if(alternatePopup!=null){alternatePopup.dismiss();alternatePopup=null;}deleteTimer.removeCallbacks(deleteRepeat);heldDelete=null;}
 private void repeatDelete(){if(!active||heldDelete==null||!heldDelete.isAttachedToWindow()||!isInputViewShown()){stopDeleteRepeat();return;}delete();deleteTimer.postDelayed(deleteRepeat,75);}
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
