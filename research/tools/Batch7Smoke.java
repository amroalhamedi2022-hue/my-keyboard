import com.zerokeyboard.ime.glide.FrequencyLexicon;
import com.zerokeyboard.ime.voice.VoiceOptions;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class Batch7Smoke {
 static int checks;
 static void check(boolean b){if(!b)throw new AssertionError("check "+checks);checks++;}
 static List<String> parse(String text,boolean ar)throws Exception{return FrequencyLexicon.parse(text.getBytes(StandardCharsets.UTF_8),ar);}
 interface Task{void run()throws Exception;}
 static void reject(Task t)throws Exception{try{t.run();throw new AssertionError("expected rejection");}catch(IOException expected){checks++;}}
 public static void main(String[] args)throws Exception{
  check(parse("\uFEFFHello 9\nworld 8\nHELLO 2\nword 0\n! 99\nعربي 50\ncat 8\n",false).equals(Arrays.asList("hello","cat","world")));
  check(parse("بدي 100\nمرحبا 20\nأهلا 30\nhello 900\n١٢ 100\n",true).equals(Arrays.asList("بدي","أهلا","مرحبا")));
  check(parse("cat 2\ncat 20\ndog 10\n",false).equals(Arrays.asList("cat","dog")));
  reject(()->parse("a 100\n! 10\n",false));reject(()->parse("hello 10\n",true));reject(()->FrequencyLexicon.parse(new byte[]{(byte)0xC3,0x28},false));
  reject(()->FrequencyLexicon.read(new ByteArrayInputStream(new byte[FrequencyLexicon.MAX_BYTES+1])));
  reject(()->parse("cat 1\n".repeat(50001),false));
  StringBuilder large=new StringBuilder();for(int i=0;i<9000;i++){int n=i;StringBuilder word=new StringBuilder("word");for(int j=0;j<4;j++){word.append((char)('a'+n%26));n/=26;}large.append(word).append(' ').append(i+1).append('\n');}
  List<String> limited=parse(large.toString(),false);check(limited.size()==8000);check(limited.get(0).equals("worddina"));
  check(FrequencyLexicon.merge(Arrays.asList("cat","dog"),Arrays.asList("bird","cat"),Arrays.asList("dog","fish")).equals(Arrays.asList("bird","cat","dog","fish")));
  VoiceOptions v=new VoiceOptions(99,999,true,1);check(v.language==0&&v.threads==4&&v.silenceMillis==400&&v.endpoint);
  v=new VoiceOptions(2,-1,false,9999);check(v.language==2&&v.threads==1&&v.silenceMillis==3000&&!v.endpoint);
  System.out.println("PASS: "+checks+" resource and voice-option checks");
 }
}
