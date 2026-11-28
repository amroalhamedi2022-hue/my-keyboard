package com.zerokeyboard.ime;
/** Provisional semantic palette/geometry tokens, separate from layout and input logic. */
public final class ThemeTokens {
 public final int background,key,text,accent,press,radius,spacing,fontSize,trailWidth;
 private ThemeTokens(boolean dark,int radius,int spacing,int fontSize,int trailWidth){
  background=dark?0xFF181D26:0xFFE8EDF3;key=dark?0xFF313A49:0xFFFFFFFF;text=dark?0xFFFFFFFF:0xFF18212D;
  accent=dark?0xFF76D8FF:0xFF1458B0;press=dark?0x35FFFFFF:0x240C3970;
  this.radius=clamp(radius,0,20);this.spacing=clamp(spacing,0,5);this.fontSize=clamp(fontSize,14,24);this.trailWidth=clamp(trailWidth,1,8);
 }
 public static ThemeTokens resolve(int mode,boolean systemDark,int radius,int spacing,int fontSize,int trailWidth){return new ThemeTokens(mode==1||(mode!=2&&systemDark),radius,spacing,fontSize,trailWidth);}
 private static int clamp(int value,int min,int max){return Math.max(min,Math.min(max,value));}
 private static double channel(int c){double x=c/255.0;return x<=0.04045?x/12.92:Math.pow((x+0.055)/1.055,2.4);}
 private static double luminance(int c){return 0.2126*channel((c>>16)&255)+0.7152*channel((c>>8)&255)+0.0722*channel(c&255);}
 public static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+0.05)/(Math.min(x,y)+0.05);}
}
