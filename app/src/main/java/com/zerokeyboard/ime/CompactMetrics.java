package com.zerokeyboard.ime;
/** Compact, predictable main keyboard geometry. Toolbar/number row never consume a full letter row. */
public final class CompactMetrics {
 private CompactMetrics(){}
 public static int keyHeight(int heightDp,int desired,boolean landscape,int rows,int toolbar,int number){
  int budget=(int)(Math.max(1,heightDp)*(landscape?.48:.36));
  return Math.max(32,Math.min(desired,(budget-toolbar-number-12)/Math.max(1,rows)-4));
 }
 public static int toolbar(int height){return Math.max(32,Math.min(40,height*3/4));}
 public static float indent(int row,int keyCount,boolean arabic,boolean special){return special||arabic?0:row==1?.5f:row==2?.25f:0;}
}
