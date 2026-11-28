package com.zerokeyboard.ime.tools;
import java.util.*;
/** Original small categorized Unicode seed; no network, fonts or imported assets. */
public final class EmojiCatalog {
 private EmojiCatalog(){}
 private static final String[][] GROUPS={
  {"😀","😃","😄","😁","😆","😅","😂","🙂","🙃","😉","😊","😍","🥰","😘","😎","🤔","😴","😭","😢","😡","😮","🤩","🥳","😇"},
  {"👍","👎","👏","🙌","🙏","🤝","👋","✌️","👌","🤲","💪","🫶","❤️","💙","💚","💛","🤍","🖤","💜","💔","✨","🔥","⭐","💯"},
  {"🐱","🐶","🐦","🦋","🐝","🌹","🌷","🌻","🌿","🌳","🌴","🌙","☀️","🌧️","🌈","🌊","🍎","🍌","🍇","🍉","☕","🍵","🍕","🍰"},
  {"📱","💻","⌚","📚","✏️","📝","📌","🔑","🔒","💡","📷","🎧","🚗","✈️","🏠","🏫","⚽","🎮","🎉","🎁","✅","❌","⚠️","❓"}
 };
 public static String[] group(int n){return GROUPS[Math.max(0,Math.min(GROUPS.length-1,n))].clone();}
 public static int count(){return GROUPS.length;}
}
