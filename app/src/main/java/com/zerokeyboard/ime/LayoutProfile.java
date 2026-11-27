package com.zerokeyboard.ime;
/** Pure layout policy. Dimensions are dp; platform view geometry stays separate. */
public final class LayoutProfile {
 public enum Mode { FULL, SPLIT, LEFT, RIGHT }
 public final Mode mode;
 public final int keyHeight,sideInset,gap;
 private LayoutProfile(Mode m,int h,int i,int g){mode=m;keyHeight=h;sideInset=i;gap=g;}
 public static LayoutProfile resolve(int widthDp,boolean landscape,boolean tablet,Mode requested,int heightPercent){
  int width=Math.max(1,widthDp);
  int scale=Math.max(75,Math.min(125,heightPercent));
  Mode mode=requested==null?Mode.FULL:requested;
  if(mode==Mode.SPLIT&&width<600)mode=Mode.FULL;
  int base=landscape?(tablet?48:40):(tablet?56:48);
  int height=Math.max(32,base*scale/100);
  int inset=mode==Mode.LEFT||mode==Mode.RIGHT?Math.max(0,width-Math.min(width,Math.max(280,width*70/100))):0;
  int gap=mode==Mode.SPLIT?Math.min(160,width*14/100):0;
  return new LayoutProfile(mode,height,inset,gap);
 }
 public static String digits(boolean indic){return indic?"١ ٢ ٣ ٤ ٥ ٦ ٧ ٨ ٩ ٠":"1 2 3 4 5 6 7 8 9 0";}
 public static String localizeDigits(String text,boolean indic){
  if(!indic)return text;StringBuilder b=new StringBuilder(text.length());
  for(int i=0;i<text.length();i++){char c=text.charAt(i);b.append(c>='0'&&c<='9'?(char)('٠'+c-'0'):c);}return b.toString();
 }
}
