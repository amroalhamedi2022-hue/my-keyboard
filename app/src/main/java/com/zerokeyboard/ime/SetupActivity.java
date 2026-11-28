package com.zerokeyboard.ime;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.Manifest;
import android.content.pm.PackageManager;
import com.zerokeyboard.ime.voice.*;
import com.zerokeyboard.ime.glide.DictionaryResourceStore;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
public final class SetupActivity extends Activity {
 private final ExecutorService imports=Executors.newSingleThreadExecutor();
 private TextView modelStatus;
 private ModelStore models;
 private DictionaryResourceStore dictionaries;
 private TextView dictionaryStatus;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
  int pad=(int)(24*getResources().getDisplayMetrics().density); root.setPadding(pad,pad,pad,pad);
  TextView title=new TextView(this); title.setText(R.string.app_name); title.setTextSize(30); root.addView(title);
  TextView info=new TextView(this); info.setText(R.string.setup_info); root.addView(info);
  Button enable=new Button(this); enable.setText(R.string.enable_keyboard); enable.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))); root.addView(enable);
  Button select=new Button(this); select.setText(R.string.choose_keyboard); select.setOnClickListener(v->getSystemService(InputMethodManager.class).showInputMethodPicker()); root.addView(select);
  EditText test=new EditText(this); test.setHint(R.string.try_typing); test.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE); root.addView(test);
  ZeroSettings settings=new ZeroSettings(this);
  toggle(root,"English glide",settings.glide(false),settings,"glide_en");
  toggle(root,"Arabic glide",settings.glide(true),settings,"glide_ar");
  toggle(root,"Glide trail",settings.trail(),settings,"glide_trail");
  toggle(root,"Glide automatic space",settings.space(),settings,"glide_space");
  toggle(root,"Incognito (disables glide and voice)",settings.incognito(),settings,"incognito");
  toggle(root,"Number row",settings.numberRow(),settings,"number_row");
  toggle(root,"Arabic-Indic numbers",settings.indic(),settings,"arabic_indic");
  toggle(root,"Spacebar cursor gesture",settings.spaceCursor(),settings,"space_cursor");
  if(android.os.Build.VERSION.SDK_INT>=30)toggle(root,"Show Android Autofill suggestions",settings.inlineAutofill(),settings,"inline_autofill");
  toggle(root,"Editor toolbar",settings.editorTools(),settings,"editor_tools");
  toggle(root,"Personal glide dictionary",settings.personalEnabled(),settings,"personal_enabled");
  dictionary(root,settings,false);dictionary(root,settings,true);
  dictionaries=new DictionaryResourceStore(this);
  toggle(root,"Use imported frequency dictionary",settings.importedDictionary(),settings,"imported_dictionary");
  dictionaryStatus=new TextView(this);dictionaryStatus.setText("Local UTF-8 word/count dictionaries • up to 8,000 words per language");root.addView(dictionaryStatus);
  for(boolean ar:new boolean[]{false,true}){
   Button add=new Button(this);add.setText(ar?"Import Arabic frequency list":"Import English frequency list");add.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,ar?45:44);});root.addView(add);
   Button remove=new Button(this);remove.setText(ar?"Remove Arabic frequency list":"Remove English frequency list");remove.setOnClickListener(v->imports.execute(()->{dictionaries.remove(ar);runOnUiThread(()->{if(!isDestroyed())dictionaryStatus.setText("Dictionary removed");});}));root.addView(remove);
  }
  toggle(root,"Session clipboard (explicit save only)",settings.clipboard(),settings,"clipboard_history");
  TextView retention=new TextView(this);retention.setText("Clipboard expiry: "+settings.clipboardTtl()/60000+" min");root.addView(retention);
  SeekBar expiry=new SeekBar(this);expiry.setMax(59);expiry.setProgress((int)(settings.clipboardTtl()/60000)-1);
  expiry.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int n,boolean user){retention.setText("Clipboard expiry: "+(n+1)+" min");if(user)settings.clipboardMinutes(n+1);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});root.addView(expiry);
  toggle(root,"Emoji panel",settings.emoji(),settings,"emoji_panel");
  selector(root,"Emoji skin tone",new String[]{"Default","Light","Medium-light","Medium","Medium-dark","Dark"},settings.emojiTone(),n->settings.number("emoji_tone",n));
  selector(root,"Keyboard appearance",new String[]{"System","Dark","Light"},settings.themeMode(),n->settings.number("theme_mode",n));
  range(root,"Key corner radius",0,20,settings.keyRadius(),n->settings.number("key_radius",n));
  range(root,"Key spacing",0,5,settings.keySpacing(),n->settings.number("key_spacing",n));
  range(root,"Key font size",14,24,settings.keyFont(),n->settings.number("key_font",n));
  range(root,"Gesture trail width",1,8,settings.trailWidth(),n->settings.number("trail_width",n));
  toggle(root,"Key vibration",settings.keyHaptic(),settings,"key_haptic");
  toggle(root,"Key click sound",settings.keySound(),settings,"key_sound");
  toggle(root,"Continuous segmented dictation (experimental)",settings.continuousVoice(),settings,"voice_continuous");
  toggle(root,"Stop single-utterance dictation after silence",settings.stopOnSilence(),settings,"voice_endpoint");
  VoiceOptions voiceOptions=settings.voiceOptions();
  selector(root,"Voice language (Auto recommended)",new String[]{"Auto / mixed experimental","Arabic","English"},voiceOptions.language,n->settings.number("voice_language",n));
  selector(root,"Voice CPU threads",new String[]{"1","2","3","4"},voiceOptions.threads-1,n->settings.number("voice_threads",n+1));
  TextView silence=new TextView(this);silence.setText("Silence duration: "+voiceOptions.silenceMillis+" ms");root.addView(silence);
  SeekBar duration=new SeekBar(this);duration.setMax(26);duration.setProgress((voiceOptions.silenceMillis-400)/100);
  duration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int n,boolean user){int value=400+n*100;silence.setText("Silence duration: "+value+" ms");if(user)settings.number("voice_silence_ms",value);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});root.addView(duration);
  toggle(root,"Review dictated text before insertion",settings.voiceReview(),settings,"voice_review");
  for(boolean tablet:new boolean[]{false,true})for(boolean landscape:new boolean[]{false,true})profile(root,settings,tablet,landscape);
  models=new ModelStore(this);modelStatus=new TextView(this);root.addView(modelStatus);refreshModels();
  toggle(root,"Enable experimental offline voice",settings.voice(),settings,"voice_enabled");
  toggle(root,"Use Base rather than Tiny model",settings.model()==ModelCatalog.BASE,settings,"voice_base");
  Button mic=new Button(this);mic.setText(R.string.grant_microphone);mic.setOnClickListener(v->{if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},42);});root.addView(mic);
  Button imp=new Button(this);imp.setText(R.string.import_model);imp.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,43);});root.addView(imp);
  for(ModelCatalog model:ModelCatalog.values()){Button del=new Button(this);del.setText(getString(R.string.remove_model,model.name()));del.setOnClickListener(v->{models.delete(model);refreshModels();});root.addView(del);}
  ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
 }
 private void refreshModels(){modelStatus.setText(getString(R.string.model_status,models.installed(ModelCatalog.TINY)?getString(R.string.installed):getString(R.string.missing),models.installed(ModelCatalog.BASE)?getString(R.string.installed):getString(R.string.missing)));}
 @Override protected void onActivityResult(int req,int result,Intent data){
  super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
  if(req==44||req==45){android.net.Uri resource=data.getData();dictionaryStatus.setText("Validating dictionary…");
   imports.execute(()->{try{int count=dictionaries.install(resource,req==45);runOnUiThread(()->{if(!isDestroyed())dictionaryStatus.setText("Dictionary installed: "+count+" valid words");});}catch(Exception e){runOnUiThread(()->{if(!isDestroyed())dictionaryStatus.setText("Dictionary rejected: invalid or too large");});}});return;
  }
  if(req!=43)return;
  android.net.Uri uri=data.getData();modelStatus.setText(R.string.verifying_model);
  imports.execute(()->{try{ModelCatalog model=models.install(uri);runOnUiThread(()->{if(!isDestroyed()){refreshModels();Toast.makeText(this,getString(R.string.model_verified,model.name()),Toast.LENGTH_SHORT).show();}});}catch(Exception e){runOnUiThread(()->{if(!isDestroyed()){refreshModels();Toast.makeText(this,R.string.model_rejected,Toast.LENGTH_LONG).show();}});}});
 }
 @Override protected void onDestroy(){imports.shutdown();super.onDestroy();}
 private void range(LinearLayout root,String title,int min,int max,int current,java.util.function.IntConsumer change){
  TextView label=new TextView(this);SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(Math.max(0,Math.min(max-min,current-min)));
  label.setText(title+": "+(min+bar.getProgress()));root.addView(label);root.addView(bar);
  bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int n,boolean user){label.setText(title+": "+(min+n));if(user)change.accept(min+n);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
 }
 private void selector(LinearLayout root,String title,String[] labels,int current,java.util.function.IntConsumer change){
  TextView label=new TextView(this);label.setText(title);root.addView(label);
  Spinner spinner=new Spinner(this);spinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels));spinner.setSelection(Math.max(0,Math.min(labels.length-1,current)));
  spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,android.view.View view,int n,long id){change.accept(n);}public void onNothingSelected(AdapterView<?> parent){}});root.addView(spinner);
 }
 private void dictionary(LinearLayout root,ZeroSettings settings,boolean arabic){
  Button edit=new Button(this);edit.setText(arabic?"Edit Arabic personal words":"Edit English personal words");
  edit.setOnClickListener(v->{EditText words=new EditText(this);words.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);words.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);words.setText(settings.personalText(arabic));words.setMinLines(4);words.setMaxLines(8);words.setHint("One word per line");
   android.app.AlertDialog d=new android.app.AlertDialog.Builder(this).setTitle(arabic?"Arabic words":"English words").setMessage("Explicit personal vocabulary only. Up to 512 words; no automatic learning.").setView(words).setPositiveButton("Save",null).setNegativeButton("Cancel",null).setNeutralButton("Clear",null).create();
   d.setOnShowListener(dialog->{d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{try{settings.personalText(arabic,words.getText().toString());d.dismiss();}catch(IllegalArgumentException e){words.setError("Use only letters of the selected language, 2–32 per word.");}});d.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(b->words.setText(""));});d.show();
  });root.addView(edit);
 }
 private void profile(LinearLayout root,ZeroSettings settings,boolean tablet,boolean landscape){
  TextView label=new TextView(this);label.setText(getString(R.string.profile_label,getString(tablet?R.string.tablet:R.string.phone),getString(landscape?R.string.landscape:R.string.portrait)));root.addView(label);
  Spinner modes=new Spinner(this);String[] names={"Full","Split (wide screens)","One hand left","One hand right"};
  modes.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));modes.setSelection(settings.layout(tablet,landscape).ordinal());
  modes.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> parent,android.view.View view,int pos,long id){settings.layout(tablet,landscape,LayoutProfile.Mode.values()[pos]);}public void onNothingSelected(android.widget.AdapterView<?> parent){}});root.addView(modes);
  SeekBar height=new SeekBar(this);height.setMax(50);height.setProgress(Math.max(0,Math.min(50,settings.height(tablet,landscape)-75)));
  TextView value=new TextView(this);value.setText(getString(R.string.key_height,75+height.getProgress()));root.addView(value);
  height.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){value.setText(getString(R.string.key_height,75+p));if(user)settings.height(tablet,landscape,75+p);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});root.addView(height);
 }
 private void toggle(LinearLayout root,String title,boolean initial,ZeroSettings settings,String key){
  Switch item=new Switch(this);item.setText(title);item.setChecked(initial);item.setOnCheckedChangeListener((v,value)->settings.set(key,value));root.addView(item);
 }
}
