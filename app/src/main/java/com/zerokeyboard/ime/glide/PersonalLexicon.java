package com.zerokeyboard.ime.glide;
import java.text.Normalizer;
import java.util.*;
/** Explicit user vocabulary only. No typing history or automatic learning. */
public final class PersonalLexicon {
 public static final int MAX_WORDS=512, MAX_CHARS=32768;
 private PersonalLexicon(){}
 public static List<String> parse(String text,boolean arabic){
  if(text==null||text.length()>MAX_CHARS)throw new IllegalArgumentException("dictionary too large");
  LinkedHashSet<String> out=new LinkedHashSet<>();
  for(String token:text.split("[\\s,،;؛]+")){
   if(token.isEmpty())continue;
   String word=Normalizer.normalize(token,Normalizer.Form.NFC);
   if(!arabic)word=word.toLowerCase(Locale.ROOT);
   int n=word.codePointCount(0,word.length());
   if(n<2||n>32)throw new IllegalArgumentException("word length must be 2–32 letters");
   for(int i=0;i<word.length();){int cp=word.codePointAt(i);i+=Character.charCount(cp);
    if(arabic?!((cp>=0x0621&&cp<=0x063A)||(cp>=0x0641&&cp<=0x064A)):!(cp>='a'&&cp<='z'))throw new IllegalArgumentException("word contains unsupported characters");}
   out.add(word);if(out.size()>MAX_WORDS)throw new IllegalArgumentException("too many words");
  }
  return Collections.unmodifiableList(new ArrayList<>(out));
 }
 public static List<String> merge(Collection<String> seed,Collection<String> personal){
  LinkedHashSet<String> out=new LinkedHashSet<>(personal);out.addAll(seed);return Collections.unmodifiableList(new ArrayList<>(out));
 }
}
