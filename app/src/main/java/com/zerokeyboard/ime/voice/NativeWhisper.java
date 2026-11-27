package com.zerokeyboard.ime.voice;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
public final class NativeWhisper {
 private static final AtomicLong IDS=new AtomicLong();
 private static final boolean READY;
 static{boolean loaded=false;try{System.loadLibrary("zero_whisper");loaded=true;}catch(LinkageError ignored){}READY=loaded;}
 private NativeWhisper(){}
 public static boolean available(){return READY;}
 public static long newSession(){return IDS.incrementAndGet();}
 public static void cancel(long id){if(READY)abort(id);}
 public static String transcribe(String model,float[] pcm,int count,long id){
  if(!READY)throw new IllegalStateException("voice library unavailable");
  byte[] bytes=decode(model,pcm,count,Math.min(4,Math.max(1,Runtime.getRuntime().availableProcessors())),id);
  if(bytes==null)throw new IllegalStateException("offline inference failed");
  return new String(bytes,StandardCharsets.UTF_8).trim();
 }
 private static native byte[] decode(String path,float[] pcm,int count,int threads,long id);
 private static native void abort(long id);
}
