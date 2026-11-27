package com.zerokeyboard.ime.voice;
import java.nio.charset.StandardCharsets;
public final class NativeWhisper {
 private static final boolean READY;
 static{boolean loaded=false;try{System.loadLibrary("zero_whisper");loaded=true;}catch(LinkageError ignored){}READY=loaded;}
 private NativeWhisper(){}
 public static boolean available(){return READY;}
 public static String transcribe(String model,float[] pcm,int count){
  if(!READY)throw new IllegalStateException("voice library unavailable");
  byte[] bytes=decode(model,pcm,count,Math.min(4,Math.max(1,Runtime.getRuntime().availableProcessors())));
  if(bytes==null)throw new IllegalStateException("offline inference failed");
  return new String(bytes,StandardCharsets.UTF_8).trim();
 }
 private static native byte[] decode(String path,float[] pcm,int count,int threads);
}
