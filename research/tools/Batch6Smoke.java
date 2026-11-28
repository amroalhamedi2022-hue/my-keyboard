import com.zerokeyboard.ime.glide.PersonalLexicon;
import com.zerokeyboard.ime.tools.ClipboardHistory;
import java.util.*;
public class Batch6Smoke {
 private static void invalid(String s,boolean ar){try{PersonalLexicon.parse(s,ar);throw new AssertionError("accepted invalid word");}catch(IllegalArgumentException expected){}}
 public static void main(String[] args){
  assert PersonalLexicon.parse("Hello,HELLO world",false).equals(Arrays.asList("hello","world"));
  assert PersonalLexicon.parse("بدي،مرحبا؛أهلا",true).equals(Arrays.asList("بدي","مرحبا","أهلا"));
  assert PersonalLexicon.parse("",true).isEmpty();
  invalid("abc123",false);invalid("hello مرحبا",false);invalid("hello",true);invalid("a",false);invalid("x".repeat(33),false);invalid("x".repeat(32769),false);
  assert PersonalLexicon.merge(Arrays.asList("cat","dog"),Arrays.asList("dog","bird")).equals(Arrays.asList("dog","bird","cat"));
  ClipboardHistory h=new ClipboardHistory();long ttl=1000;
  assert !h.save("secret",true,0,ttl);
  assert !h.save("123456",false,0,ttl);
  assert !h.save("code ١٢٣٤٥٦",false,0,ttl);
  assert !h.save("1234",false,0,ttl);
  assert !h.save("x".repeat(2049),false,0,ttl);
  assert h.save("مرحبا hello",false,0,ttl);
  long id=h.list(0,ttl).get(0).id;
  assert h.save("مرحبا hello",false,500,ttl)&&h.list(500,ttl).size()==1;
  assert h.list(1499,ttl).size()==1;
  h.pin(id,true,1499,ttl);assert h.list(5000,ttl).size()==1;
  h.pin(id,false,5000,ttl);assert h.list(5000,ttl).isEmpty();
  for(int i=0;i<12;i++)assert h.save("word "+i,false,5000,ttl);
  long newest=h.list(5000,ttl).get(0).id;h.pin(newest,true,5000,ttl);
  assert h.save("extra",false,5000,ttl)&&h.list(5000,ttl).size()==12;
  assert h.list(5000,ttl).stream().anyMatch(e->e.id==newest&&e.pinned);
  h.remove(newest);assert h.list(5000,ttl).size()==11;
  h.clear();assert h.list(5000,ttl).isEmpty();
  System.out.println("PASS: personal lexicon and clipboard history synthetic smoke checks");
 }
}
