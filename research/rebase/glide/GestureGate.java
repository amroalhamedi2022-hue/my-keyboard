package com.zerokeyboard.ime.glide;
import java.util.HashSet;
import java.util.Set;
/** Touch admission only: ordinary taps remain owned by the mature keyboard. */
public final class GestureGate {
 private final Set<Integer> letters=new HashSet<>();
 private long started; private float x0,y0; private boolean tracking,claimed;
 public void begin(int code,float x,float y,long time){reset();if(code<=0||!Character.isLetter(code))return;tracking=true;started=time;x0=x;y0=y;letters.add(Character.toLowerCase(code));}
 public boolean move(int code,float x,float y,long time,float unit){
  if(!tracking)return false;
  if(code>0&&Character.isLetter(code))letters.add(Character.toLowerCase(code));
  if(!claimed&&letters.size()>=3&&time-started>=65&&time-started<=1200&&Math.hypot(x-x0,y-y0)>=Math.max(1,unit)*1.35)claimed=true;
  return claimed;
 }
 public boolean claimed(){return claimed;}
 public boolean tracking(){return tracking;}
 public void reset(){tracking=claimed=false;letters.clear();}
}
