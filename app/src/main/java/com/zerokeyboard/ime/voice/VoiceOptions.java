package com.zerokeyboard.ime.voice;
/** Immutable per-session tuning. Auto remains default; forced language is experimental. */
public final class VoiceOptions {
 public final int language,threads,silenceMillis;
 public final boolean endpoint;
 public VoiceOptions(int language,int threads,boolean endpoint,int silenceMillis){
  this.language=language>=0&&language<=2?language:0;
  this.threads=Math.max(1,Math.min(4,threads));this.endpoint=endpoint;
  this.silenceMillis=Math.max(400,Math.min(3000,silenceMillis));
 }
}
