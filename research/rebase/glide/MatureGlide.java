// SPDX-License-Identifier: GPL-3.0-only
package com.zerokeyboard.ime.glide;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.inputmethod.InputConnection;
import helium314.keyboard.keyboard.MainKeyboardView;
import helium314.keyboard.keyboard.Keyboard;
import helium314.keyboard.keyboard.Key;
import java.util.*;
import java.util.concurrent.*;

public final class MatureGlide implements AutoCloseable {
 public interface Host {
  boolean allowed();
  SharedPreferences settings();
  InputConnection target();
  void commit(InputConnection target,String word);
 }
 private final Host host;
 private final DictionaryResourceStore dictionaries;
 private final GestureGate gate=new GestureGate();
 private final GeometricRanker ranker=new GeometricRanker();
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1),new ThreadPoolExecutor.DiscardOldestPolicy());
 private final List<GeometricRanker.Point> trace=new ArrayList<>();
 private Keyboard keyboard;
 private float unit;
 private volatile long generation;
 private boolean consuming,closed;
 public MatureGlide(Context context,Host host){this.host=host;dictionaries=new DictionaryResourceStore(context);}
 public void invalidate(){generation++;worker.getQueue().clear();}
 private void reset(){gate.reset();trace.clear();keyboard=null;}
 public void cancel(){invalidate();reset();consuming=false;}
 public void close(){closed=true;cancel();worker.shutdownNow();}
 private int keyAt(MainKeyboardView view,float x,float y){
  int localX=(int)(x-view.getPaddingLeft()),localY=(int)(y-view.getPaddingTop());
  for(Key key:keyboard.getSortedKeys())if(key.getHitBox().contains(localX,localY))return key.getCode();
  return 0;
 }
 private void sample(MainKeyboardView view,float x,float y){
  if(trace.size()<512)trace.add(new GeometricRanker.Point((x-view.getPaddingLeft())/unit,(y-view.getPaddingTop())/unit));
 }
 public boolean onTouch(MainKeyboardView view,MotionEvent event,boolean popup){
  int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){
   cancel();
   if(closed||popup||!host.allowed()||!host.settings().getBoolean("mature_glide_enabled",false))return false;
   Keyboard current=view.getKeyboard();if(current==null||!current.mId.isAlphabetKeyboard())return false;
   String language=current.mId.getLocale().getLanguage();if(!language.equals("ar")&&!language.equals("en"))return false;
   if(!host.settings().getBoolean(language.equals("ar")?"glide_ar":"glide_en",true))return false;
   keyboard=current;unit=Math.max(1,current.mOccupiedWidth/10f);
   gate.begin(keyAt(view,event.getX(),event.getY()),event.getX(),event.getY(),event.getEventTime());
   if(gate.tracking())sample(view,event.getX(),event.getY());else reset();
   return false;
  }
  if(!gate.tracking()&&!consuming)return false;
  if(popup||action==MotionEvent.ACTION_CANCEL||event.getPointerCount()!=1||view.getKeyboard()!=keyboard||!host.allowed()){
   boolean claimed=consuming;reset();invalidate();
   if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_UP)consuming=false;
   return claimed;
  }
  if(action==MotionEvent.ACTION_MOVE){
   if(!consuming&&gate.move(keyAt(view,event.getX(),event.getY()),event.getX(),event.getY(),event.getEventTime(),unit)){
    consuming=true;view.cancelAllOngoingEvents();
   }
   for(int i=0;i<event.getHistorySize();i++)sample(view,event.getHistoricalX(i),event.getHistoricalY(i));
   sample(view,event.getX(),event.getY());return consuming;
  }
  if(action==MotionEvent.ACTION_UP){
   boolean claimed=consuming;
   if(claimed&&keyboard!=null&&Character.isLetter(keyAt(view,event.getX(),event.getY()))&&trace.size()<512){
    sample(view,event.getX(),event.getY());submit();
   }
   reset();consuming=false;return claimed;
  }
  return consuming;
 }
 private void submit(){
  if(keyboard==null||closed)return;
  InputConnection target=host.target();if(target==null)return;
  long token=++generation;boolean arabic=keyboard.mId.getLocale().getLanguage().equals("ar");
  Map<Integer,GeometricRanker.Point> centers=new HashMap<>();
  for(Key key:keyboard.getSortedKeys()){
   int code=key.getCode();if(code>0&&Character.isLetter(code))centers.put(Character.toLowerCase(code),new GeometricRanker.Point((key.getX()+key.getWidth()/2f)/unit,(key.getY()+key.getHeight()/2f)/unit));
  }
  List<GeometricRanker.Point> points=new ArrayList<>(trace);
  SharedPreferences prefs=host.settings();String personalText=prefs.getBoolean("personal_enabled",true)?prefs.getString(arabic?"personal_ar":"personal_en",""):"";
  boolean imported=prefs.getBoolean("imported_dictionary",true),space=prefs.getBoolean("glide_space",true);
  worker.execute(()->{
   if(token!=generation)return;
   try{
    List<String> personal;try{personal=PersonalLexicon.parse(personalText,arabic);}catch(IllegalArgumentException e){personal=Collections.emptyList();}
    List<String> words=FrequencyLexicon.merge(SeedLexicon.words(arabic),personal,dictionaries.words(arabic,imported));
    List<GeometricRanker.Candidate> candidates=ranker.rank(points,centers,words,3);
    if(candidates.isEmpty()||candidates.get(0).score>2.0)return;
    String result=candidates.get(0).word+(space?" ":"");
    main.post(()->{if(!closed&&token==generation&&host.allowed()&&target==host.target())host.commit(target,result);});
   }catch(RuntimeException ignored){}
  });
 }
}
