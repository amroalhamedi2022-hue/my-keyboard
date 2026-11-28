package com.zerokeyboard.ime;
/** A space touch is either a tap, cursor movement, language switch, or cancellation. */
public final class SpaceGesture {
 public enum Mode { CURSOR, LANGUAGE, OFF }
 public enum End { SPACE, SWITCH, NONE }
 private final Mode mode; private final float threshold,step; private float origin,last; private boolean moved,cancelled,started;
 public SpaceGesture(Mode mode,float threshold,float step){this.mode=mode;this.threshold=Math.max(1,threshold);this.step=Math.max(1,step);}
 public void begin(float x){origin=last=x;moved=cancelled=false;started=true;}
 public int move(float x){if(!started||cancelled)return 0;if(Math.abs(x-origin)>=threshold)moved=true;if(!moved||mode!=Mode.CURSOR)return 0;int n=(int)((x-last)/step);n=Math.max(-8,Math.min(8,n));last+=n*step;return n;}
 public void cancel(){cancelled=true;}
 public End end(float x,boolean inside){if(!started||cancelled){started=false;return End.NONE;}started=false;if(Math.abs(x-origin)>=threshold)moved=true;if(mode==Mode.LANGUAGE&&Math.abs(x-origin)>=threshold)return End.SWITCH;return !moved&&inside?End.SPACE:End.NONE;}
}
