package com.zerokeyboard.ime;
import android.content.Context;
import android.graphics.*;
import android.widget.Button;
/** Crisp monochrome vector glyphs, independent of device emoji fonts. */
final class KeyButton extends Button {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path path=new Path();
 KeyButton(Context c){super(c);}
 @Override protected void onDraw(Canvas canvas){
  String label=getText().toString();if(!("⌫ ⇧ ⇪ ↵ ⚙ 🎙 ☺ ▣ ⋯ ‹".contains(label)&&!label.isEmpty()&&label.codePointCount(0,label.length())==1)){super.onDraw(canvas);return;}
  float unit=Math.min(Math.min(getWidth(),getHeight())*.48f,24*getResources().getDisplayMetrics().density)/24f;
  if(unit<=0)return;int save=canvas.save();canvas.translate(getWidth()/2f-12*unit,getHeight()/2f-12*unit);canvas.scale(unit,unit);
  paint.setColor(getCurrentTextColor());paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.9f);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);
  switch(label){
   case "⌫":poly(canvas,2,12,8,5,22,5,22,19,8,19,2,12);line(canvas,11,9,17,15);line(canvas,17,9,11,15);break;
   case "⇧":case "⇪":poly(canvas,3,11,12,3,21,11,16,11,16,20,8,20,8,11,3,11);if(label.equals("⇪"))line(canvas,8,23,16,23);break;
   case "↵":poly(canvas,21,5,21,13,5,13);poly(canvas,10,8,5,13,10,18);break;
   case "🎙":canvas.drawRoundRect(9,2,15,14,3,3,paint);canvas.drawArc(6,6,18,18,0,180,false,paint);line(canvas,12,18,12,22);line(canvas,8,22,16,22);break;
   case "⚙":canvas.drawCircle(12,12,7,paint);canvas.drawCircle(12,12,2.8f,paint);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(canvas,(float)(12+7*Math.cos(a)),(float)(12+7*Math.sin(a)),(float)(12+10*Math.cos(a)),(float)(12+10*Math.sin(a)));}break;
   case "☺":canvas.drawCircle(12,12,9,paint);canvas.drawArc(7,8,17,17,10,160,false,paint);dot(canvas,9,9);dot(canvas,15,9);break;
   case "▣":canvas.drawRoundRect(5,5,19,21,2,2,paint);canvas.drawRoundRect(8,2,16,8,2,2,paint);line(canvas,9,12,15,12);line(canvas,9,16,15,16);break;
   case "⋯":dot(canvas,5,12);dot(canvas,12,12);dot(canvas,19,12);break;
   case "‹":poly(canvas,15,5,8,12,15,19);break;
  }
  canvas.restoreToCount(save);
 }
 private void line(Canvas c,float x,float y,float xx,float yy){c.drawLine(x,y,xx,yy,paint);}
 private void poly(Canvas c,float... coords){path.reset();path.moveTo(coords[0],coords[1]);for(int i=2;i<coords.length;i+=2)path.lineTo(coords[i],coords[i+1]);c.drawPath(path,paint);}
 private void dot(Canvas c,float x,float y){paint.setStyle(Paint.Style.FILL);c.drawCircle(x,y,1.6f,paint);paint.setStyle(Paint.Style.STROKE);}
}
