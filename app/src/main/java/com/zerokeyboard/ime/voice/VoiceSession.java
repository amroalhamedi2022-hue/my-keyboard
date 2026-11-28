package com.zerokeyboard.ime.voice;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.*;
import android.os.*;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
/** Bounded single or segmented continuous dictation. All state callbacks on main thread, samples only in RAM. */
public final class VoiceSession {
 public interface Listener {
  void state(String state);
  void result(String text);
  default void partial(String text) {}
  default void level(int percent) {}
 }
 private static final int RATE=16000,MAX=RATE*30;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private static final class Job {
  volatile boolean stop,cancel,paused;
  final long id=NativeWhisper.newSession();
  final Listener listener;
  String result,status;
  Job(Listener listener){this.listener=listener;}
 }
 private final AtomicBoolean running=new AtomicBoolean();
 private Job current;
 private boolean closed;
 public boolean busy(){return current!=null;}
 public void start(Context context,ModelStore store,ModelCatalog model,VoiceOptions options,boolean continuous,Listener listener){
  if(closed||current!=null)return;
  if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){listener.state("Voice unavailable • microphone permission required");return;}
  if(!running.compareAndSet(false,true)){listener.state("Voice unavailable • previous session ending");return;}
  Job job=new Job(listener);current=job;listener.state("Preparing…");
  worker.execute(()->{
   try{record(context,job,store,model,options,continuous);}
   finally{running.set(false);publishCompletion(job);}
  });
 }
 public void togglePause(){if(current!=null&&!current.stop)current.paused=!current.paused;}
 public void stop(){if(current!=null)current.stop=true;}
 public void cancel(){if(current!=null){current.cancel=true;current.stop=true;NativeWhisper.cancel(current.id);current=null;}}
 public void close(){closed=true;cancel();worker.shutdownNow();}
 private void state(Job job,String s){main.post(()->{if(current==job&&!job.cancel)job.listener.state(s);});}
 private void partial(Job job,String text){main.post(()->{if(current==job&&!job.cancel)job.listener.partial(text);});}
 private void level(Job job,int percent){main.post(()->{if(current==job&&!job.cancel)job.listener.level(percent);});}
 private void done(Job job,String text,String status){job.result=text;job.status=status;}
 private void publishCompletion(Job job){
  main.post(()->{
   if(current!=job||job.cancel)return;
   current=null;
   job.listener.state(job.status==null?"Voice unavailable • check setup":job.status);
   if(!job.cancel&&job.result!=null&&!job.result.isEmpty())job.listener.result(job.result);
  });
 }
 private void record(Context context,Job job,ModelStore store,ModelCatalog model,VoiceOptions options,boolean continuous){
  float[] samples=null;short[] chunk=null;AudioRecord recorder=null;int count=0;
  AudioSegments segments=null;SegmentedDecoder pipeline=null;AudioSegments.Chunk backlog=null,tail=null;
  try{
   if(job.cancel||job.stop){done(job,null,"Ready");return;}
   if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("microphone permission revoked");
   File file=store.verified(model);if(job.cancel||job.stop){done(job,null,"Ready");return;}
   if(!NativeWhisper.available())throw new IllegalStateException("library missing");
   int min=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);if(min<=0)throw new IllegalStateException("audio format unavailable");
   if(continuous){segments=new AudioSegments(options.silenceMillis);pipeline=new SegmentedDecoder(c->NativeWhisper.transcribe(file.getAbsolutePath(),c.pcm,c.pcm.length,job.id,options),text->partial(job,text));}
   else {samples=new float[MAX];}
   chunk=new short[1024];
   recorder=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,4096));
   if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("mic unavailable");
   if(job.cancel||job.stop){done(job,null,"Ready");return;}
   recorder.startRecording();if(recorder.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException("recording denied");
   state(job,"Recording • tap to stop");
   long deadline=SystemClock.elapsedRealtime()+(continuous?300000:120000);SpeechWindow speech=new SpeechWindow();boolean recording=true;
   long nextMeterUpdate=0;
   while(!job.stop&&!job.cancel&&(continuous||count<MAX)&&SystemClock.elapsedRealtime()<deadline){
    if(continuous&&pipeline.done())throw new IllegalStateException("decoder stopped early");
    if(job.paused){
     if(recording){recorder.stop();recording=false;level(job,0);state(job,"Paused • microphone off");}
     Thread.sleep(25);continue;
    }
    if(!recording){
     if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("microphone permission revoked");
     if(job.stop||job.cancel)break;
     recorder.startRecording();if(recorder.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException("resume denied");
     recording=true;state(job,"Recording • tap to stop");
    }
    int n=recorder.read(chunk,0,continuous?chunk.length:Math.min(chunk.length,MAX-count),AudioRecord.READ_NON_BLOCKING);
    if(n<0)throw new IllegalStateException("audio read failed");
    if(n==0){Thread.sleep(12);continue;}
    long now=SystemClock.elapsedRealtime();
    if(now>=nextMeterUpdate){
     int peak=0;
     for(int i=0;i<n;i++)peak=Math.max(peak,Math.abs((int)chunk[i]));
     level(job,Math.min(100,peak*100/8192));
     nextMeterUpdate=now+100;
    }
    if(continuous){
     for(int i=0;i<n;i++){AudioSegments.Chunk ready=segments.accept(chunk[i]);if(ready!=null&&!pipeline.offer(ready)){
      if(backlog!=null){ready.close();throw new IllegalStateException("unexpected second backlog");}
      backlog=ready;job.stop=true;
     }}
    }else{
     for(int i=0;i<n;i++){float x=chunk[i]/32768f;samples[count++]=x;speech.accept(chunk[i]);}
     if(options.endpoint&&speech.ended(options.silenceMillis))break;
    }
   }
   if(recording)recorder.stop();recorder.release();recorder=null;
   if(job.cancel)return;
   if(continuous){
    boolean stoppedForBacklog=backlog!=null;
    state(job,stoppedForBacklog?"Processing locally… • decoder backlog":"Processing locally…");
    if(backlog!=null){enqueueLast(pipeline,backlog,job);backlog=null;}
    tail=segments.finish();if(tail!=null){enqueueLast(pipeline,tail,job);tail=null;}
    pipeline.finish();String result=pipeline.await(180,TimeUnit.SECONDS);
    done(job,result,stoppedForBacklog?"Ready • capture stopped due to decoder backlog":result.isEmpty()?"No speech detected":"Ready");return;
   }
   if(!speech.detected()||speech.end()-speech.start()<1600){done(job,null,"No speech detected");return;}
   state(job,"Processing locally…");
   int length=speech.end()-speech.start();
   System.arraycopy(samples,speech.start(),samples,0,length);
   String result=NativeWhisper.transcribe(file.getAbsolutePath(),samples,length,job.id,options);
   done(job,result,result.isEmpty()?"No speech detected":"Ready");
  }catch(Exception|LinkageError error){String recovered=pipeline==null?"":pipeline.completedText();done(job,recovered.isEmpty()?null:recovered,recovered.isEmpty()?"Voice unavailable • check setup":"Ready • partial result after decoder interruption");}
  finally{if(recorder!=null){try{recorder.stop();}catch(RuntimeException ignored){}try{recorder.release();}catch(RuntimeException ignored){}}if(pipeline!=null){if(!pipeline.done())NativeWhisper.cancel(job.id);pipeline.close();}if(backlog!=null)backlog.close();if(tail!=null)tail.close();if(segments!=null)segments.close();if(samples!=null)Arrays.fill(samples,0);if(chunk!=null)Arrays.fill(chunk,(short)0);NativeWhisper.release(job.id);}
 }
 private void enqueueLast(SegmentedDecoder pipeline,AudioSegments.Chunk chunk,Job job)throws Exception{
  long deadline=SystemClock.elapsedRealtime()+120000;
  while(!job.cancel&&!pipeline.done()&&SystemClock.elapsedRealtime()<deadline){
   if(pipeline.offer(chunk))return;Thread.sleep(25);
  }
  throw new IllegalStateException("decoder queue unavailable");
 }
}
