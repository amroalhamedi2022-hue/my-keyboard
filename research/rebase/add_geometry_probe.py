"""Evaluation-only key geometry export, never used in owner release build."""
import sys
from pathlib import Path
from prepare_candidate import replace
root=Path(sys.argv[1])
service=root/"app/src/main/java/com/zerokeyboard/ime/ZeroImeService.java"
if "VoiceSession voice" not in service.read_text(): raise ValueError("voice adapter required")
replace(service,"super.onStartInputView(info,restarting);","super.onStartInputView(info,restarting); queueGeometry();")
replace(service,"        cancelVoice(); super.onEvent(event);","        cancelVoice(); super.onEvent(event); queueGeometry();")
replace(service, '''control(controls,"Discard",this::cancelVoice);''',
        '''control(controls,"Discard",this::cancelVoice).setTag("zero-eval-discard");''')
replace(service, '''                meter.setVisibility(recording ? View.VISIBLE : View.INVISIBLE);''',
        '''                meter.setVisibility(recording ? View.VISIBLE : View.INVISIBLE);
                evaluationRecording = recording;
                queueGeometry();''')
replace(service, '''        preview=null;meter=null;pause=null;stop=null;insert=null;''',
        '''        preview=null;meter=null;pause=null;stop=null;insert=null;
        evaluationRecording = false;
        queueGeometry();''')
replace(service,"    private boolean allowed(){",'''    // Evaluation-only static key geometry. Never reads the editor or records typed input.
    private boolean evaluationRecording;
    private final android.os.Handler geometryHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private void queueGeometry(){geometryHandler.removeCallbacks(geometry);geometryHandler.postDelayed(geometry,250);}
    private final Runnable geometry=this::writeGeometry;
    private void writeGeometry(){
        try{
            var view=helium314.keyboard.keyboard.KeyboardSwitcher.getInstance().getMainKeyboardView();
            org.json.JSONArray result=new org.json.JSONArray();
            if (isInputViewShown() && view!=null && view.isShown() && view.getKeyboard()!=null) {
                int[] origin=new int[2];view.getLocationOnScreen(origin);
                for(var key:view.getKeyboard().getSortedKeys()){
                    result.put(evaluationPoint(key.getCode(),
                            origin[0]+view.getPaddingLeft()+key.getX()+key.getWidth()/2,
                            origin[1]+view.getPaddingTop()+key.getY()+key.getHeight()/2));
                }
                if (anchor != null) {
                    var pinned = anchor.findViewById(R.id.pinned_keys);
                    var toolbar = anchor.findViewById(R.id.toolbar);
                    var tag = helium314.keyboard.latin.utils.ToolbarKey.VOICE;
                    appendEvaluationView(result, -233, pinned == null ? null : pinned.findViewWithTag(tag));
                    appendEvaluationView(result, -233, toolbar == null ? null : toolbar.findViewWithTag(tag));
                }
                if (panel != null && panel.isShowing()) {
                    appendEvaluationView(result, -10001, panel.getContentView().findViewWithTag("zero-eval-discard"));
                    if (evaluationRecording) result.put(evaluationPoint(-10002, 0, 0));
                }
            }
            java.io.File file=new java.io.File(getApplicationContext().getFilesDir(),"zero-eval-geometry.json");
            android.util.AtomicFile atomic=new android.util.AtomicFile(file);java.io.FileOutputStream stream=atomic.startWrite();
            try{stream.write(result.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));atomic.finishWrite(stream);}catch(Exception e){atomic.failWrite(stream);}
        }catch(Exception ignored){}
    }
    private void appendEvaluationView(org.json.JSONArray result, int code, View view) throws org.json.JSONException {
        if (view == null || !view.isShown() || !view.isEnabled() || view.getWidth() == 0 || view.getHeight() == 0) return;
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        result.put(evaluationPoint(code, location[0] + view.getWidth()/2, location[1] + view.getHeight()/2));
    }
    private org.json.JSONObject evaluationPoint(int code, int x, int y) throws org.json.JSONException {
        return new org.json.JSONObject().put("code", code).put("x", x).put("y", y);
    }
    private boolean allowed(){''')
replace(service,"    @Override public void onDestroy(){ cancelVoice();","    @Override public void onDestroy(){ cancelVoice(); geometryHandler.removeCallbacks(geometry);")
