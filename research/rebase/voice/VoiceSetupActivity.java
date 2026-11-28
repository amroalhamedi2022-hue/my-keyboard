// SPDX-License-Identifier: GPL-3.0-only
package com.zerokeyboard.ime;
import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import com.zerokeyboard.ime.voice.*;
import java.util.concurrent.*;

public final class VoiceSetupActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private ModelStore models;private SharedPreferences prefs;private TextView status;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        Context storage=getApplicationContext();
        prefs=storage.getSharedPreferences("zero_settings",MODE_PRIVATE);models=new ModelStore(storage);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(16),dp(20),dp(20));
        TextView title=new TextView(this);title.setText("Zero Voice");title.setTextSize(28);root.addView(title);
        TextView description=new TextView(this);description.setText("Local Arabic and English transcription. No audio uploaded. Voice is unavailable in private or incognito fields. Disable the keyboard's incognito mode only if you want to use voice in ordinary fields.");root.addView(description);
        toggle(root,"Enable offline voice","voice_enabled",false);
        toggle(root,"Use Base model","voice_base",false);
        toggle(root,"Continuous segmented dictation","voice_continuous",false);
        toggle(root,"Stop single utterance after silence","voice_endpoint",false);
        toggle(root,"Review text before insertion","voice_review",true);
        status=new TextView(this);root.addView(status);refresh();
        button(root,"Grant microphone permission",()->requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},81));
        button(root,"Get selected model in browser",()->{
            boolean base=prefs.getBoolean("voice_base",false);
            String rev=base?"f281eb45af861ab5e5297d23694b7d46e090c02c":"0b364b566045a405be7225ee1e415a073e04da77";
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://huggingface.co/ggerganov/whisper.cpp/resolve/"+rev+"/"+(base?ModelCatalog.BASE:ModelCatalog.TINY).filename)));}catch(ActivityNotFoundException e){status.setText("No browser available");}
        });
        button(root,"Import verified model (.bin)",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,82);});
        button(root,"Remove selected model",()->{ModelCatalog m=prefs.getBoolean("voice_base",false)?ModelCatalog.BASE:ModelCatalog.TINY;worker.execute(()->{models.delete(m);runOnUiThread(()->{if(!isDestroyed())refresh();});});});
        button(root,"Return to keyboard",this::finish);
        ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void toggle(LinearLayout root,String text,String key,boolean def){Switch s=new Switch(this);s.setText(text);s.setChecked(prefs.getBoolean(key,def));s.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean(key,on).apply());root.addView(s);}
    private void button(LinearLayout root,String text,Runnable action){Button b=new Button(this);b.setAllCaps(false);b.setText(text);b.setOnClickListener(v->action.run());root.addView(b);}
    private void refresh(){status.setText("Tiny: "+(models.installed(ModelCatalog.TINY)?"installed":"missing")+" | Base: "+(models.installed(ModelCatalog.BASE)?"installed":"missing"));}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=82||result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();status.setText("Verifying model…");worker.execute(()->{try{models.install(uri);runOnUiThread(()->{if(!isDestroyed())refresh();});}catch(Exception e){runOnUiThread(()->{if(!isDestroyed())status.setText("Model rejected: not a verified supported model");});}});}
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
}
