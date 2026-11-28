// SPDX-License-Identifier: GPL-3.0-only
package com.zerokeyboard.ime;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.*;
import com.zerokeyboard.ime.voice.*;
import helium314.keyboard.event.Event;
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode;
import helium314.keyboard.latin.R;
import helium314.keyboard.latin.settings.Settings;

/** Local voice adapter. Upstream owns every ordinary key, layout and editing operation. */
public final class ZeroImeService extends helium314.keyboard.latin.LatinIME {
    private VoiceSession voice;
    private View anchor;
    private View panel;
    private TextView preview;
    private ProgressBar meter;
    private Button pause, stop, insert;
    private String pending="";
    private long generation;
    private InputConnection target;
    private boolean processing;
    private SharedPreferences prefs() {
        return getApplicationContext().getSharedPreferences("zero_settings", MODE_PRIVATE);
    }
    private Context storage() { return getApplicationContext(); }
    @Override public void onCreate(){ super.onCreate(); voice=new VoiceSession(); }
    @Override public void setInputView(View view){ cancelVoice(); super.setInputView(view); anchor=view; }
    @Override public void onStartInput(EditorInfo info,boolean restarting){ cancelVoice(); super.onStartInput(info,restarting); }
    @Override public void onStartInputView(EditorInfo info,boolean restarting){ cancelVoice(); super.onStartInputView(info,restarting); }
    @Override public void onFinishInputView(boolean finishing){ cancelVoice(); super.onFinishInputView(finishing); }
    @Override public void onFinishInput(){ cancelVoice(); super.onFinishInput(); }
    @Override public void onWindowHidden(){ cancelVoice(); super.onWindowHidden(); }
    @Override public void onConfigurationChanged(Configuration c){ cancelVoice(); super.onConfigurationChanged(c); }
    @Override public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype){ cancelVoice(); super.onCurrentInputMethodSubtypeChanged(subtype); }
    @Override public void onDestroy(){ cancelVoice(); if(voice!=null)voice.close(); super.onDestroy(); }
    @Override public void onUpdateSelection(int oldStart,int oldEnd,int start,int end,int composingStart,int composingEnd){
        if(oldStart!=start||oldEnd!=end)cancelVoice();
        super.onUpdateSelection(oldStart,oldEnd,start,end,composingStart,composingEnd);
    }
    @Override public void onStartBatchInput(){ cancelVoice(); super.onStartBatchInput(); }
    @Override public void onTextInput(String text){ cancelVoice(); super.onTextInput(text); }
    @Override public void onEvent(Event event){
        if(event.getKeyCode()==KeyCode.VOICE_INPUT){ toggleVoice(); return; }
        cancelVoice(); super.onEvent(event);
    }
    private boolean allowed(){
        if(voice==null||!isInputViewShown()||!EditorPolicy.from(getCurrentInputEditorInfo()).allowPersonalization)return false;
        try{
            return !Settings.getInstance().getCurrent().mIncognitoModeEnabled && !prefs().getBoolean("incognito",false);
        }catch(RuntimeException e){return false;}
    }
    private void requestMicrophone(){
        cancelVoice();
        Intent i = new Intent(this, VoicePermissionActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try{startActivity(i);}catch(android.content.ActivityNotFoundException e){notice(R.string.zero_voice_engine_unavailable);}
    }
    private void notice(int message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private void toggleVoice(){
        if(!allowed() || !Settings.getInstance().getCurrent().mShowsVoiceInputKey){
            notice(R.string.zero_voice_private);
            return;
        }
        if(voice.busy()){if(processing)cancelVoice();else voice.stop();return;}
        if(panel!=null){cancelVoice();return;}
        SharedPreferences p=prefs();
        if(!p.getBoolean("voice_enabled",true)){notice(R.string.zero_voice_disabled);return;}
        if(!NativeWhisper.available()){notice(R.string.zero_voice_engine_unavailable);return;}
        ModelStore models=new ModelStore(storage());
        ModelCatalog model=p.getBoolean("voice_base",false)?ModelCatalog.BASE:ModelCatalog.TINY;
        if(!models.available(model)){
            if(model != ModelCatalog.BASE || !models.available(ModelCatalog.TINY)){
                notice(R.string.zero_voice_model_missing);
                return;
            }
            model=ModelCatalog.TINY;
            notice(R.string.zero_voice_model_fallback);
        }
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestMicrophone();return;}
        target=getCurrentInputConnection(); if(target==null)return;
        if(!showPanel()){cancelVoice();return;}
        long id=++generation;
        VoiceOptions options=new VoiceOptions(p.getInt("voice_language",0),p.getInt("voice_threads",4),p.getBoolean("voice_endpoint",true),p.getInt("voice_silence_ms",1200));
        voice.start(storage(),models,model,options,p.getBoolean("voice_continuous",false),new VoiceSession.Listener(){
            public void state(String state){
                if(id!=generation||panel==null)return;
                if(!allowed()){cancelVoice();return;}
                processing=state.startsWith("Processing");
                boolean recording=state.startsWith("Recording"),paused=state.startsWith("Paused");
                preview.setText(state);
                meter.setVisibility(recording ? View.VISIBLE : View.INVISIBLE);
                if (!recording) meter.setProgress(0);
                pause.setEnabled(recording||paused); pause.setText(paused?"Resume":"Pause");
                stop.setEnabled(recording||paused||state.startsWith("Preparing")||processing);
                stop.setText(processing?"Cancel":"Stop");
                if(state.startsWith("Voice unavailable")||state.startsWith("No speech")){Toast.makeText(ZeroImeService.this,state,Toast.LENGTH_SHORT).show();cancelVoice();}
            }
            public void partial(String text){if(id==generation&&panel!=null&&allowed())preview.setText(text);}
            public void level(int percent){if(id==generation&&meter!=null)meter.setProgress(percent);}
            public void result(String text){
                if(id!=generation||panel==null||!allowed()||target!=getCurrentInputConnection()){cancelVoice();return;}
                pending=text; preview.setText(text); pause.setEnabled(false);stop.setEnabled(false);insert.setEnabled(!text.isEmpty());
                if(!prefs().getBoolean("voice_review",false))commitPending();
            }
        });
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private Button control(LinearLayout row,String label,Runnable action){
        Button b=new Button(this); b.setAllCaps(false);b.setText(label);b.setTextSize(12);b.setOnClickListener(v->action.run());
        row.addView(b,new LinearLayout.LayoutParams(0,dp(42),1));return b;
    }
    private boolean showPanel(){
        if (!(anchor instanceof FrameLayout frame) || !frame.isAttachedToWindow()
                || frame.getHeight() < dp(96) || frame.getWidth() <= dp(16)) return false;
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(8),dp(12),dp(8));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                night ? new int[]{0xFF26344A, 0xFF121A29} : new int[]{0xFFFAFCFF, 0xFFE9EEF7});
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), night ? 0xFF455674 : 0xFFD6E3F7);
        root.setBackground(bg);
        root.setClickable(true);
        root.setElevation(dp(6));
        preview=new TextView(this);preview.setTextColor(night ? 0xFFF2F6FF : 0xFF19283D);preview.setTextSize(15);preview.setMaxLines(3);preview.setText("Preparing offline voice…");
        root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
        meter = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        meter.setMax(100);
        meter.setProgressTintList(android.content.res.ColorStateList.valueOf(night ? 0xFF9EC3FF : 0xFF2256B5));
        meter.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        meter.setVisibility(View.INVISIBLE);
        root.addView(meter, new LinearLayout.LayoutParams(-1, dp(5)));
        LinearLayout controls=new LinearLayout(this);root.addView(controls);
        pause=control(controls,"Pause",()->voice.togglePause());stop=control(controls,"Stop",()->{if(processing)cancelVoice();else voice.stop();});
        insert=control(controls,"Insert",this::commitPending);control(controls,"Discard",this::cancelVoice);
        pause.setEnabled(false);insert.setEnabled(false);
        int width = Math.min(frame.getWidth() - dp(16), dp(520));
        int height = Math.min(frame.getHeight() - dp(12), dp(124));
        FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(
                width, height, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        layout.bottomMargin = dp(8);
        frame.addView(root, layout);
        panel = root;
        return true;
    }
    private void commitPending(){
        String text=pending;boolean valid=allowed()&&target!=null&&target==getCurrentInputConnection()&&!text.isEmpty();cancelVoice();
        if(valid)super.onTextInput(text);
    }
    private void cancelVoice(){
        generation++;pending="";target=null;processing=false;
        if(voice!=null)voice.cancel();
        View old = panel;
        panel = null;
        if (old != null && old.getParent() instanceof FrameLayout parent) parent.removeView(old);
        preview=null;meter=null;pause=null;stop=null;insert=null;
    }
}
