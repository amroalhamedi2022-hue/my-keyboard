package com.zerokeyboard.ime;
import android.content.Context;
import android.view.*;
import android.widget.LinearLayout;
/** Extends only the tiny physical gaps between keys. Never crosses a split keyboard gap. */
final class KeyRow extends LinearLayout {
 private View delegated;
 private final float tolerance;
 KeyRow(Context c){super(c);tolerance=3*c.getResources().getDisplayMetrics().density;}
 @Override public boolean dispatchTouchEvent(MotionEvent e){
  int action=e.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){
   delegated=null;float x=e.getX(),y=e.getY();float best=Float.MAX_VALUE;
   for(int i=0;i<getChildCount();i++){View v=getChildAt(i);if(v.getVisibility()!=VISIBLE||!v.isClickable()||!v.isEnabled())continue;
    if(x>=v.getLeft()&&x<v.getRight()&&y>=v.getTop()&&y<v.getBottom())return super.dispatchTouchEvent(e);
    float dx=Math.max(0,Math.max(v.getLeft()-x,x-v.getRight())),dy=Math.max(0,Math.max(v.getTop()-y,y-v.getBottom()));
    float d=dx*dx+dy*dy;if(dx<=tolerance&&dy<=tolerance&&d<best){best=d;delegated=v;}
   }
  }
  if(delegated==null)return super.dispatchTouchEvent(e);
  View target=delegated;MotionEvent local=MotionEvent.obtain(e);local.offsetLocation(-target.getLeft(),-target.getTop());
  if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_UP){
   if(local.getX()>=-tolerance&&local.getX()<=target.getWidth()+tolerance&&local.getY()>=-tolerance&&local.getY()<=target.getHeight()+tolerance)
    local.setLocation(Math.max(0,Math.min(target.getWidth()-1,local.getX())),Math.max(0,Math.min(target.getHeight()-1,local.getY())));
  }
  if(action==MotionEvent.ACTION_POINTER_DOWN)local.setAction(MotionEvent.ACTION_CANCEL);
  boolean handled=target.dispatchTouchEvent(local);local.recycle();
  if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN)delegated=null;
  return handled;
 }
}
