package com.zerokeyboard.ime.voice;
import android.media.*;
import android.os.*;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
/** Single bounded utterance. All state callbacks on main thread, samples only in RAM. */
public final class VoiceSession {
 public interface Listener{void state(String state);void result(String text);}
 private static final int RATE=16000,MAX=RATE*30;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private static final class Job {volatile boolean stop,cancel;final long id=NativeWhisper.newSession();final Listener listener;Job(Listener l){listener=l;}}
 private final AtomicBoolean running=new AtomicBoolean();
 private Job current;
 private boolean closed;
 public boolean busy(){return current!=null;}
 public void start(ModelStore store,ModelCatalog model,Listener listener){
  if(closed||current!=null)return;
  if(!running.compareAndSet(false,true)){listener.state("Voice unavailable • previous session ending");return;}
  Job job=new Job(listener);current=job;listener.state("Preparing…");
  worker.execute(()->{try{record(job,store,model);}finally{running.set(false);}});
 }
 public void stop(){if(current!=null)current.stop=true;}
 public void cancel(){if(current!=null){current.cancel=true;current.stop=true;NativeWhisper.cancel(current.id);current=null;}}
 public void close(){closed=true;cancel();worker.shutdownNow();}
 private void state(Job job,String s){main.post(()->{if(current==job&&!job.cancel)job.listener.state(s);});}
 private void done(Job job,String text,String status){main.post(()->{if(current!=job||job.cancel)return;current=null;job.listener.state(status);if(text!=null&&!text.isEmpty())job.listener.result(text);});}
 private void record(Job job,ModelStore store,ModelCatalog model){
  float[] samples=null;short[] chunk=null;AudioRecord recorder=null;int count=0;
  try{
   if(job.cancel||job.stop){done(job,null,"Ready");return;}
   File file=store.verified(model);if(job.cancel||job.stop){done(job,null,"Ready");return;}
   if(!NativeWhisper.available())throw new IllegalStateException("library missing");
   int min=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);if(min<=0)throw new IllegalStateException("audio format unavailable");
   samples=new float[MAX];chunk=new short[1024];
   recorder=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,4096));
   if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("mic unavailable");
   if(job.cancel||job.stop){done(job,null,"Ready");return;}
   recorder.startRecording();if(recorder.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException("recording denied");
   state(job,"Recording • tap to stop");
   long deadline=SystemClock.elapsedRealtime()+30000;float peak=0;
   while(!job.stop&&!job.cancel&&count<MAX&&SystemClock.elapsedRealtime()<deadline){
    int n=recorder.read(chunk,0,Math.min(chunk.length,MAX-count),AudioRecord.READ_NON_BLOCKING);
    if(n<0)throw new IllegalStateException("audio read failed");
    if(n==0){Thread.sleep(12);continue;}
    for(int i=0;i<n;i++){float x=chunk[i]/32768f;samples[count++]=x;peak=Math.max(peak,Math.abs(x));}
   }
   recorder.stop();recorder.release();recorder=null;
   if(job.cancel)return;
   if(count<RATE/3||peak<0.003f){done(job,null,"No speech detected");return;}
   state(job,"Processing locally…");
   String result=NativeWhisper.transcribe(file.getAbsolutePath(),samples,count,job.id);
   done(job,result,"Ready");
  }catch(Exception|LinkageError error){done(job,null,"Voice unavailable • check setup");}
  finally{if(recorder!=null){try{recorder.stop();}catch(RuntimeException ignored){}recorder.release();}if(samples!=null)Arrays.fill(samples,0);if(chunk!=null)Arrays.fill(chunk,(short)0);}
 }
}
