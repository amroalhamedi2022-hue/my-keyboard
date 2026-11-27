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
 private void toggle(LinearLayout root,String title,boolean initial,ZeroSettings settings,String key){
  Switch item=new Switch(this);item.setText(title);item.setChecked(initial);item.setOnCheckedChangeListener((v,value)->settings.set(key,value));root.addView(item);
 }
}
