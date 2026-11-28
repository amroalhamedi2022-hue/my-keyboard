package com.zerokeyboard.ime;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.LinearLayout;
import com.zerokeyboard.ime.glide.GeometricRanker.Point;
import java.util.*;
/** Preserves native button clicks until a qualifying letter-to-letter drag intercepts them. */
@android.annotation.SuppressLint("ViewConstructor")
final class GlideSurface extends LinearLayout {
 interface Listener {void onGlide(List<Point> trace,Map<Integer,Point> centers);}
 private final Listener listener;
 private final Map<View,String> letters=new LinkedHashMap<>();
 private final ArrayList<Point> points=new ArrayList<>();
 private final Map<Integer,Point> centers=new HashMap<>();
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path path=new Path();
 private final int[] origin=new int[2],location=new int[2];
 private boolean enabled,showTrail,tracking,dragging;
 private int pointer=-1;
 private float downX,downY,unit=1,lastX,lastY;
 private View first;
 GlideSurface(Context c,Listener l){super(c);listener=l;setWillNotDraw(false);paint.setColor(0xFF76D8FF);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeWidth(4*getResources().getDisplayMetrics().density);}
 void trailStyle(int color,int widthDp){paint.setColor(color);paint.setStrokeWidth(widthDp*getResources().getDisplayMetrics().density);invalidate();}
 void configure(boolean active,boolean trail){reset();letters.clear();enabled=active;showTrail=trail;}
 void register(View v,String label){if(label.codePointCount(0,label.length())==1&&Character.isLetter(label.codePointAt(0)))letters.put(v,label);}
 void reset(){tracking=false;dragging=false;pointer=-1;first=null;points.clear();centers.clear();path.reset();invalidate();}
 private View hit(float x,float y){
  for(View v:letters.keySet()){v.getLocationOnScreen(location);float l=location[0]-origin[0],t=location[1]-origin[1];if(x>=l&&x<l+v.getWidth()&&y>=t&&y<t+v.getHeight())return v;}return null;
 }
 private void geometry(){
  getLocationOnScreen(origin);
  unit=first==null?1:Math.max(1,first.getWidth());
  centers.clear();
  for(Map.Entry<View,String> e:letters.entrySet()){View v=e.getKey();v.getLocationOnScreen(location);centers.put(e.getValue().codePointAt(0),new Point((location[0]-origin[0]+v.getWidth()/2f)/unit,(location[1]-origin[1]+v.getHeight()/2f)/unit));}
 }
 private void add(float x,float y){
  if(points.size()>=512)return;
  if(!points.isEmpty()&&Math.hypot(x-lastX,y-lastY)<2)return;
  points.add(new Point(x/unit,y/unit));if(points.size()==1)path.moveTo(x,y);else path.lineTo(x,y);
  lastX=x;lastY=y;invalidate();
 }
 @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e){
  switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:
    reset();if(!enabled)return false;getLocationOnScreen(origin);
    first=hit(e.getX(),e.getY());if(first==null)return false;
    tracking=true;pointer=e.getPointerId(0);downX=e.getX();downY=e.getY();geometry();add(downX,downY);return false;
   case MotionEvent.ACTION_MOVE:
    if(!tracking)return false;int i=e.findPointerIndex(pointer);if(i<0){reset();return false;}
    float x=e.getX(i),y=e.getY(i);
    for(int h=0;h<e.getHistorySize();h++)add(e.getHistoricalX(i,h),e.getHistoricalY(i,h));
    add(x,y);
    View other=hit(x,y);
    if(other!=null&&other!=first&&Math.hypot(x-downX,y-downY)>unit*.5f){dragging=true;return true;}return false;
   case MotionEvent.ACTION_POINTER_DOWN: case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL: reset();return false;
   default:return false;
  }
 }
 @Override public boolean onTouchEvent(MotionEvent e){
  if(!dragging)return super.onTouchEvent(e);
  switch(e.getActionMasked()){
   case MotionEvent.ACTION_MOVE:
    int i=e.findPointerIndex(pointer);if(i<0){reset();return true;}
    for(int h=0;h<e.getHistorySize();h++)add(e.getHistoricalX(i,h),e.getHistoricalY(i,h));add(e.getX(i),e.getY(i));break;
   case MotionEvent.ACTION_UP:
    add(e.getX(),e.getY());List<Point> copy=new ArrayList<>(points);Map<Integer,Point> keys=new HashMap<>(centers);boolean valid=hit(e.getX(),e.getY())!=null;reset();if(valid){performClick();listener.onGlide(copy,keys);}break;
   case MotionEvent.ACTION_CANCEL: case MotionEvent.ACTION_POINTER_DOWN: reset();break;
   default:break;
  }return true;
 }
 @Override public boolean performClick(){super.performClick();return true;}
 @Override protected void dispatchDraw(Canvas canvas){super.dispatchDraw(canvas);if(dragging&&showTrail)canvas.drawPath(path,paint);}
 @Override protected void onDetachedFromWindow(){reset();super.onDetachedFromWindow();}
}
