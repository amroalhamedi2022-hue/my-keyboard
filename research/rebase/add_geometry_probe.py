"""Evaluation-only key geometry export, never used in owner release build."""
import sys
from pathlib import Path
from prepare_candidate import replace
root=Path(sys.argv[1])
service=root/"app/src/main/java/com/zerokeyboard/ime/ZeroImeService.java"
if "VoiceSession voice" not in service.read_text(): raise ValueError("voice adapter required")
replace(service,"    @Override public void onStartInputView(EditorInfo info,boolean restarting){ cancelVoice(); super.onStartInputView(info,restarting); }","    @Override public void onStartInputView(EditorInfo info,boolean restarting){ cancelVoice(); super.onStartInputView(info,restarting); queueGeometry(); }")
replace(service,"        cancelVoice(); super.onEvent(event);","        cancelVoice(); super.onEvent(event); queueGeometry();")
replace(service,"    private boolean allowed(){",'''    // Evaluation-only static key geometry. Never reads the editor or records typed input.
    private final android.os.Handler geometryHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private void queueGeometry(){geometryHandler.removeCallbacks(geometry);geometryHandler.postDelayed(geometry,250);}
    private final Runnable geometry=this::writeGeometry;
    private void writeGeometry(){
        try{
            var view=helium314.keyboard.keyboard.KeyboardSwitcher.getInstance().getMainKeyboardView();
            if(view==null||!view.isShown()||view.getKeyboard()==null)return;
            int[] origin=new int[2];view.getLocationOnScreen(origin);
            org.json.JSONArray result=new org.json.JSONArray();
            for(var key:view.getKeyboard().getSortedKeys()){
                org.json.JSONObject item=new org.json.JSONObject();
                item.put("code",key.getCode());
                item.put("x",origin[0]+view.getPaddingLeft()+key.getX()+key.getWidth()/2);
                item.put("y",origin[1]+view.getPaddingTop()+key.getY()+key.getHeight()/2);
                result.put(item);
            }
            java.io.File file=new java.io.File(getApplicationContext().getFilesDir(),"zero-eval-geometry.json");
            android.util.AtomicFile atomic=new android.util.AtomicFile(file);java.io.FileOutputStream stream=atomic.startWrite();
            try{stream.write(result.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));atomic.finishWrite(stream);}catch(Exception e){atomic.failWrite(stream);}
        }catch(Exception ignored){}
    }
    private boolean allowed(){''')
replace(service,"    @Override public void onDestroy(){ cancelVoice();","    @Override public void onDestroy(){ geometryHandler.removeCallbacks(geometry); cancelVoice();")
