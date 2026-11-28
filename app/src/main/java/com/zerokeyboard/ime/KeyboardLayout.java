package com.zerokeyboard.ime;
import java.util.List;
import java.util.Arrays;
/** Original provisional layouts, ordered by physical left-to-right key placement. */
public final class KeyboardLayout {
 private KeyboardLayout() {}
 public enum Language { EN, AR }
 public static List<String> rows(Language lang, boolean shift, boolean symbols, boolean numeric) {
  if(numeric) return Arrays.asList("1 2 3","4 5 6","7 8 9","- 0 .");
  if(symbols) return Arrays.asList("1 2 3 4 5 6 7 8 9 0","@ # $ % & * - + ( )","! ? : ; / \\ _ =");
  if(lang==Language.AR) return shift ?
    Arrays.asList("َ ً ُ ٌ ِ ٍ ْ ّ آ","أ إ ؤ ئ ء ة ى","ذ ، ؛ ؟ ٰ") :
    Arrays.asList("ض ص ث ق ف غ ع ه خ ح ج","ش س ي ب ل ا ت ن م ك ط","ئ ء ؤ ر لا ى ة و ز ظ د ذ","، . ؟");
  if(shift) return Arrays.asList("Q W E R T Y U I O P","A S D F G H J K L","Z X C V B N M , .");
  return Arrays.asList("q w e r t y u i o p","a s d f g h j k l","z x c v b n m , .");
 }
}
