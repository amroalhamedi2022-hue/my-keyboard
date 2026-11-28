package com.zerokeyboard.ime.glide;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.text.Normalizer;
import java.util.*;
/** Bounded UTF-8 word/count resource parser. No text history or executable content. */
public final class FrequencyLexicon {
 public static final int MAX_BYTES=2*1024*1024, MAX_LINES=50000, MAX_WORDS=8000;
 private FrequencyLexicon(){}
 public static byte[] read(InputStream in)throws IOException{
  if(in==null)throw new IOException("missing resource");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
  while((n=in.read(buffer))!=-1){if(n==0)continue;if(out.size()+n>MAX_BYTES)throw new IOException("resource too large");out.write(buffer,0,n);}return out.toByteArray();
 }
 public static List<String> parse(byte[] data,boolean arabic)throws IOException{
  if(data==null||data.length==0||data.length>MAX_BYTES)throw new IOException("invalid resource size");
  String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString();
  Map<String,Long> counts=new HashMap<>();int lines=0;
  try(BufferedReader reader=new BufferedReader(new StringReader(text))){String line;
   while((line=reader.readLine())!=null){if(++lines>MAX_LINES)throw new IOException("too many rows");if(line.length()>160)continue;
    if(lines==1&&line.startsWith("\uFEFF"))line=line.substring(1);
    String[] parts=line.trim().split("[ \\t]+");if(parts.length!=2||!parts[1].matches("[0-9]{1,18}"))continue;
    String word=Normalizer.normalize(parts[0],Normalizer.Form.NFC);if(!arabic)word=word.toLowerCase(Locale.ROOT);
    if(!valid(word,arabic))continue;long count=Long.parseLong(parts[1]);if(count<=0)continue;
    Long previous=counts.get(word);if(previous==null||count>previous)counts.put(word,count);
   }
  }
  if(counts.isEmpty())throw new IOException("no valid words for selected language");
  List<String> words=new ArrayList<>(counts.keySet());words.sort(Comparator.<String>comparingLong(counts::get).reversed().thenComparing(Comparator.naturalOrder()));
  return Collections.unmodifiableList(new ArrayList<>(words.subList(0,Math.min(MAX_WORDS,words.size()))));
 }
 private static boolean valid(String word,boolean ar){
  int len=word.length();if(len<2||len>32)return false;
  for(int i=0;i<len;i++){char c=word.charAt(i);if(ar?!((c>=0x0621&&c<=0x063A)||(c>=0x0641&&c<=0x064A)):!(c>='a'&&c<='z'))return false;}return true;
 }
 public static List<String> merge(Collection<String> seed,Collection<String> personal,Collection<String> imported){
  LinkedHashSet<String> words=new LinkedHashSet<>(personal);words.addAll(seed);words.addAll(imported);return Collections.unmodifiableList(new ArrayList<>(words));
 }
}
