package com.zerokeyboard.ime.voice;
import java.util.Arrays;
/** Bounded 16 kHz segmentation with silence endpoints. Not a neural VAD. */
public final class AudioSegments implements AutoCloseable {
 public static final int RATE=16000, MAX_SAMPLES=12*RATE;
 public static final class Chunk implements AutoCloseable {
  public final float[] pcm;
  private Chunk(float[] pcm){this.pcm=pcm;}
  public void close(){Arrays.fill(pcm,0f);}
 }
 private final float[] buffer=new float[MAX_SAMPLES];
 private final int silenceMillis;
 private SpeechWindow speech=new SpeechWindow();
 private int size;
 public AudioSegments(int silenceMillis){this.silenceMillis=Math.max(400,Math.min(3000,silenceMillis));}
 public Chunk accept(short sample){
  buffer[size++]=sample/32768f;speech.accept(sample);
  if(size==buffer.length||speech.ended(silenceMillis))return flush();return null;
 }
 public Chunk finish(){return flush();}
 private Chunk flush(){
  Chunk chunk=null;
  if(speech.detected()&&speech.end()-speech.start()>=1600)chunk=new Chunk(Arrays.copyOfRange(buffer,speech.start(),speech.end()));
  Arrays.fill(buffer,0,size,0f);size=0;speech=new SpeechWindow();return chunk;
 }
 public void close(){Arrays.fill(buffer,0f);size=0;speech=new SpeechWindow();}
}
