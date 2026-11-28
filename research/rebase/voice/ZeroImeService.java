// SPDX-License-Identifier: GPL-3.0-only
package com.zerokeyboard.ime;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
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
import helium314.keyboard.latin.settings.Settings;

/** Local voice adapter. Upstream owns every ordinary key, layout and editing operation. */
public final class ZeroImeService extends helium314.keyboard.latin.LatinIME {
    private VoiceSession voice;
    private View anchor;
    private PopupWindow panel;
    private TextView preview;
    private Button pause, stop, insert;
    private String pending="";
    private long generation;
    private InputConnection target;
    private boolean processing;
    private SharedPreferences prefs() {
        return createCredentialProtectedStorageContext().getSharedPreferences("zero_settings", MODE_PRIVATE);
    }
    private Context storage() { return createCredentialProtectedStorageContext(); }
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
    private void openVoiceSetup(){
        cancelVoice(); Intent i=new Intent(this,VoiceSetupActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
    private void toggleVoice(){
        if(!allowed()){Toast.makeText(this,"Voice is disabled in private or incognito fields",Toast.LENGTH_SHORT).show();return;}
        if(voice.busy()){if(processing)cancelVoice();else voice.stop();return;}
        if(panel!=null){cancelVoice();return;}
        SharedPreferences p=prefs();
        ModelCatalog model=p.getBoolean("voice_base",false)?ModelCatalog.BASE:ModelCatalog.TINY;
        ModelStore models=new ModelStore(storage());
        if(!p.getBoolean("voice_enabled",false)||!models.installed(model)||checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){openVoiceSetup();return;}
        if(!NativeWhisper.available()){Toast.makeText(this,"Offline voice engine unavailable",Toast.LENGTH_SHORT).show();return;}
        target=getCurrentInputConnection(); if(target==null)return;
        if(!showPanel()){cancelVoice();return;}
        long id=++generation;
        VoiceOptions options=new VoiceOptions(p.getInt("voice_language",0),p.getInt("voice_threads",4),p.getBoolean("voice_endpoint",false),p.getInt("voice_silence_ms",1200));
        voice.start(storage(),models,model,options,p.getBoolean("voice_continuous",false),new VoiceSession.Listener(){
            public void state(String state){
                if(id!=generation||panel==null)return;
                if(!allowed()){cancelVoice();return;}
                processing=state.startsWith("Processing");
                boolean recording=state.startsWith("Recording"),paused=state.startsWith("Paused");
                preview.setText(state);
                pause.setEnabled(recording||paused); pause.setText(paused?"Resume":"Pause");
                stop.setEnabled(recording||paused||state.startsWith("Preparing")||processing);
                stop.setText(processing?"Cancel":"Stop");
                if(state.startsWith("Voice unavailable")||state.startsWith("No speech")){Toast.makeText(ZeroImeService.this,state,Toast.LENGTH_SHORT).show();cancelVoice();}
            }
            public void partial(String text){if(id==generation&&panel!=null&&allowed())preview.setText(text);}
            public void result(String text){
                if(id!=generation||panel==null||!allowed()||target!=getCurrentInputConnection()){cancelVoice();return;}
                pending=text; preview.setText(text); pause.setEnabled(false);stop.setEnabled(false);insert.setEnabled(!text.isEmpty());
                if(!prefs().getBoolean("voice_review",true))commitPending();
            }
        });
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private Button control(LinearLayout row,String label,Runnable action){
        Button b=new Button(this); b.setAllCaps(false);b.setText(label);b.setTextSize(12);b.setOnClickListener(v->action.run());
        row.addView(b,new LinearLayout.LayoutParams(0,dp(42),1));return b;
    }
    private boolean showPanel(){
        if(anchor==null||!anchor.isAttachedToWindow()||anchor.getHeight()==0)return false;
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(10),dp(6),dp(10),dp(6));
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xFF202B39);bg.setCornerRadius(dp(14));root.setBackground(bg);
        preview=new TextView(this);preview.setTextColor(Color.WHITE);preview.setTextSize(15);preview.setMaxLines(3);preview.setText("Preparing offline voice…");
        root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout controls=new LinearLayout(this);root.addView(controls);
        pause=control(controls,"Pause",()->voice.togglePause());stop=control(controls,"Stop",()->{if(processing)cancelVoice();else voice.stop();});
        insert=control(controls,"Insert",this::commitPending);control(controls,"Discard",this::cancelVoice);
        pause.setEnabled(false);insert.setEnabled(false);
        int width=Math.max(dp(240),Math.min(anchor.getWidth(),dp(520))-dp(12));int height=dp(124);
        PopupWindow created=new PopupWindow(root,width,height,false);created.setTouchable(true);created.setOutsideTouchable(false);
        created.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);created.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));created.setElevation(dp(6));
        created.setOnDismissListener(()->{if(panel==created)cancelVoice();});panel=created;
        try{created.showAsDropDown(anchor,(anchor.getWidth()-width)/2,-anchor.getHeight()-height);return created.isShowing();}
        catch(RuntimeException e){panel=null;return false;}
    }
    private void commitPending(){
        String text=pending;boolean valid=allowed()&&target!=null&&target==getCurrentInputConnection()&&!text.isEmpty();cancelVoice();
        if(valid)super.onTextInput(text);
    }
    private void cancelVoice(){
        generation++;pending="";target=null;processing=false;
        if(voice!=null)voice.cancel();
        PopupWindow old=panel;panel=null;if(old!=null)old.dismiss();
        preview=null;pause=null;stop=null;insert=null;
    }
}
