// SPDX-License-Identifier: GPL-3.0-only
package com.zerokeyboard.ime;
import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import com.zerokeyboard.ime.voice.*;
import java.util.concurrent.*;

public final class VoiceSetupActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private ModelStore models;private SharedPreferences prefs;private TextView status;private boolean quickMic;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        quickMic=getIntent().getBooleanExtra("quick_mic",false);
        Context storage=getApplicationContext();
        prefs=storage.getSharedPreferences("zero_settings",MODE_PRIVATE);models=new ModelStore(storage);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(16),dp(20),dp(20));
        TextView title=new TextView(this);title.setText("Offline voice typing");title.setTextSize(28);root.addView(title);
        TextView description=new TextView(this);description.setText("Speak Arabic or English without sending audio online. Tap the microphone in an ordinary text field to dictate. Private and incognito fields remain blocked.");root.addView(description);
        status=new TextView(this);status.setTextSize(16);status.setPadding(0,dp(12),0,dp(12));root.addView(status);
        button(root,"Allow microphone and return",this::askMicrophone);
        toggle(root,"Enable offline voice","voice_enabled",true);
        toggle(root,"Use Base model","voice_base",false);
        toggle(root,"Continuous segmented dictation","voice_continuous",false);
        toggle(root,"Stop single utterance after silence","voice_endpoint",true);
        toggle(root,"Review text before insertion","voice_review",false);
        choice(root,"Voice language",new String[]{"Auto / mixed","Arabic","English"},"voice_language",0,0);
        choice(root,"CPU threads",new String[]{"1","2","3","4"},"voice_threads",4,1);
        TextView silence=new TextView(this);root.addView(silence);SeekBar duration=new SeekBar(this);duration.setMax(26);
        int current=Math.max(400,Math.min(3000,prefs.getInt("voice_silence_ms",1200)));duration.setProgress((current-400)/100);silence.setText("Silence: "+current+" ms");root.addView(duration);
        duration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean user){int value=400+100*progress;silence.setText("Silence: "+value+" ms");if(user)prefs.edit().putInt("voice_silence_ms",value).apply();}
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });
        refresh();
        button(root,"Optional: download selected model in browser",()->{
            boolean base=prefs.getBoolean("voice_base",false);
            String rev=base?"f281eb45af861ab5e5297d23694b7d46e090c02c":"0b364b566045a405be7225ee1e415a073e04da77";
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://huggingface.co/ggerganov/whisper.cpp/resolve/"+rev+"/"+(base?ModelCatalog.BASE:ModelCatalog.TINY).filename)));}catch(ActivityNotFoundException e){status.setText("No browser available");}
        });
        button(root,"Import verified model (.bin)",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,82);});
        button(root,"Remove installed model (bundled Tiny remains available)",()->{ModelCatalog m=prefs.getBoolean("voice_base",false)?ModelCatalog.BASE:ModelCatalog.TINY;worker.execute(()->{models.delete(m);runOnUiThread(()->{if(!isDestroyed())refresh();});});});
        button(root,"Return to keyboard",this::finish);
        button(root,"Android microphone permission settings",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
        if(quickMic&&state==null)askMicrophone();
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void toggle(LinearLayout root,String text,String key,boolean def){Switch s=new Switch(this);s.setText(text);s.setChecked(prefs.getBoolean(key,def));s.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean(key,on).apply();refresh();});root.addView(s);}
    private void button(LinearLayout root,String text,Runnable action){Button b=new Button(this);b.setAllCaps(false);b.setText(text);b.setOnClickListener(v->action.run());root.addView(b);}
    private void choice(LinearLayout root,String label,String[] values,String key,int def,int offset){TextView title=new TextView(this);title.setText(label);root.addView(title);Spinner spinner=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values);spinner.setAdapter(adapter);spinner.setSelection(Math.max(0,Math.min(values.length-1,prefs.getInt(key,def)-offset)));root.addView(spinner);spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,android.view.View view,int position,long id){prefs.edit().putInt(key,position+offset).apply();}public void onNothingSelected(AdapterView<?> parent){}});}
    private String modelStatus(ModelCatalog model){return models.installed(model)?"installed":models.bundled(model)?"included offline":"not installed";}
    private void refresh(){
        if(status==null)return;
        boolean granted=checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
        ModelCatalog selected=prefs.getBoolean("voice_base",false)?ModelCatalog.BASE:ModelCatalog.TINY;
        String ready=!prefs.getBoolean("voice_enabled",true)?"Voice is switched off.":!models.available(selected)?"Selected model is missing. Select included Tiny or import the selected model.":!granted?"Allow microphone access once.":"Ready to dictate. Return to your text field and tap the microphone.";
        status.setText(ready+"\nTiny: "+modelStatus(ModelCatalog.TINY)+" | Base: "+modelStatus(ModelCatalog.BASE)+"\nMicrophone: "+(granted?"allowed":"not allowed"));
    }
    private void askMicrophone(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){if(quickMic)finish();else refresh();return;}
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},81);
    }
    @Override protected void onResume(){super.onResume();refresh();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==81){refresh();if(quickMic&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)finish();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=82||result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();status.setText("Verifying model…");worker.execute(()->{try{models.install(uri);runOnUiThread(()->{if(!isDestroyed())refresh();});}catch(Exception e){runOnUiThread(()->{if(!isDestroyed())status.setText("Model rejected: not a verified supported model");});}});}
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
}
