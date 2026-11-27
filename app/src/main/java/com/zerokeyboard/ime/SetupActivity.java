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
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
public final class SetupActivity extends Activity {
 private final ExecutorService imports=Executors.newSingleThreadExecutor();
 private TextView modelStatus;
 private ModelStore models;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
  int pad=(int)(24*getResources().getDisplayMetrics().density); root.setPadding(pad,pad,pad,pad);
  TextView title=new TextView(this); title.setText("Zero Keyboard"); title.setTextSize(30); root.addView(title);
  TextView info=new TextView(this); info.setText("Development baseline • Arabic / English\nOffline manual typing. Experimental glide uses a small built-in word list. Experimental local voice requires a verified imported model and microphone permission."); root.addView(info);
  Button enable=new Button(this); enable.setText("1. Enable Zero Keyboard"); enable.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))); root.addView(enable);
  Button select=new Button(this); select.setText("2. Choose keyboard"); select.setOnClickListener(v->getSystemService(InputMethodManager.class).showInputMethodPicker()); root.addView(select);
  EditText test=new EditText(this); test.setHint("Try typing / جرّب الكتابة"); test.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE); root.addView(test);
  ZeroSettings settings=new ZeroSettings(this);
  toggle(root,"English glide",settings.glide(false),settings,"glide_en");
  toggle(root,"Arabic glide",settings.glide(true),settings,"glide_ar");
  toggle(root,"Glide trail",settings.trail(),settings,"glide_trail");
  toggle(root,"Glide automatic space",settings.space(),settings,"glide_space");
  toggle(root,"Incognito (disables glide and voice)",settings.incognito(),settings,"incognito");
  toggle(root,"Number row",settings.numberRow(),settings,"number_row");
  toggle(root,"Arabic-Indic numbers",settings.indic(),settings,"arabic_indic");
  toggle(root,"Editor toolbar",settings.editorTools(),settings,"editor_tools");
  toggle(root,"Review dictated text before insertion",settings.voiceReview(),settings,"voice_review");
  for(boolean tablet:new boolean[]{false,true})for(boolean landscape:new boolean[]{false,true})profile(root,settings,tablet,landscape);
  models=new ModelStore(this);modelStatus=new TextView(this);root.addView(modelStatus);refreshModels();
  toggle(root,"Enable experimental offline voice",settings.voice(),settings,"voice_enabled");
  toggle(root,"Use Base rather than Tiny model",settings.model()==ModelCatalog.BASE,settings,"voice_base");
  Button mic=new Button(this);mic.setText("Grant microphone permission");mic.setOnClickListener(v->{if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},42);});root.addView(mic);
  Button imp=new Button(this);imp.setText("Import official Tiny/Base Q5 model (.bin)");imp.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,43);});root.addView(imp);
  for(ModelCatalog model:ModelCatalog.values()){Button del=new Button(this);del.setText("Remove "+model.name()+" model");del.setOnClickListener(v->{models.delete(model);refreshModels();});root.addView(del);}
  ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
 }
 private void refreshModels(){modelStatus.setText("Models: Tiny "+(models.installed(ModelCatalog.TINY)?"installed":"missing")+" | Base "+(models.installed(ModelCatalog.BASE)?"installed":"missing"));}
 @Override protected void onActivityResult(int req,int result,Intent data){
  super.onActivityResult(req,result,data);if(req!=43||result!=RESULT_OK||data==null||data.getData()==null)return;
  android.net.Uri uri=data.getData();modelStatus.setText("Verifying model…");
  imports.execute(()->{try{ModelCatalog model=models.install(uri);runOnUiThread(()->{if(!isDestroyed()){refreshModels();Toast.makeText(this,model.name()+" verified",Toast.LENGTH_SHORT).show();}});}catch(Exception e){runOnUiThread(()->{if(!isDestroyed()){refreshModels();Toast.makeText(this,"Import rejected: invalid or unavailable file",Toast.LENGTH_LONG).show();}});}});
 }
 @Override protected void onDestroy(){imports.shutdown();super.onDestroy();}
 private void profile(LinearLayout root,ZeroSettings settings,boolean tablet,boolean landscape){
  TextView label=new TextView(this);label.setText((tablet?"Tablet":"Phone")+" / "+(landscape?"Landscape":"Portrait"));root.addView(label);
  Spinner modes=new Spinner(this);String[] names={"Full","Split (wide screens)","One hand left","One hand right"};
  modes.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));modes.setSelection(settings.layout(tablet,landscape).ordinal());
  modes.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> parent,android.view.View view,int pos,long id){settings.layout(tablet,landscape,LayoutProfile.Mode.values()[pos]);}public void onNothingSelected(android.widget.AdapterView<?> parent){}});root.addView(modes);
  SeekBar height=new SeekBar(this);height.setMax(50);height.setProgress(Math.max(0,Math.min(50,settings.height(tablet,landscape)-75)));
  TextView value=new TextView(this);value.setText("Key height: "+(75+height.getProgress())+"%");root.addView(value);
  height.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){value.setText("Key height: "+(75+p)+"%");if(user)settings.height(tablet,landscape,75+p);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});root.addView(height);
 }
 private void toggle(LinearLayout root,String title,boolean initial,ZeroSettings settings,String key){
  Switch item=new Switch(this);item.setText(title);item.setChecked(initial);item.setOnCheckedChangeListener((v,value)->settings.set(key,value));root.addView(item);
 }
}
