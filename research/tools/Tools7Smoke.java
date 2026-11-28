import com.zerokeyboard.ime.ThemeTokens;
import com.zerokeyboard.ime.tools.EmojiCatalog;
import com.zerokeyboard.ime.tools.ClipboardHistory;
import java.util.*;
public class Tools7Smoke {
 static int checks;static void check(boolean b){if(!b)throw new AssertionError("check "+checks);checks++;}
 public static void main(String[] args){
  for(boolean night:new boolean[]{false,true})for(int mode=0;mode<3;mode++){
   ThemeTokens t=ThemeTokens.resolve(mode,night,-1,999,100,0);check(t.radius==0&&t.spacing==5&&t.fontSize==24&&t.trailWidth==1);check(ThemeTokens.contrast(t.key,t.text)>=4.5);check(ThemeTokens.contrast(t.background,t.text)>=4.5);check((t.background==0xFF181D26)==(mode==1||(mode==0&&night)));
  }
  check(EmojiCatalog.count()==4);for(int i=0;i<4;i++)check(EmojiCatalog.group(i).length==24);
  check(EmojiCatalog.search("coffee",16).contains("☕"));check(EmojiCatalog.search("قهوة",16).contains("☕"));check(EmojiCatalog.search("قَلْب أَحْمَر",16).equals(Arrays.asList("❤️")));check(EmojiCatalog.search("blue heart",16).equals(Arrays.asList("💙")));
  check(EmojiCatalog.search("",16).isEmpty());check(EmojiCatalog.search("heart",2).size()==2);check(EmojiCatalog.skinTone("👍",3).equals("👍🏽"));check(EmojiCatalog.skinTone("✌️",1).equals("✌🏻"));check(EmojiCatalog.skinTone("❤️",3).equals("❤️"));check(EmojiCatalog.skinTone("👍🏽",5).equals("👍🏽"));
  check(!ClipboardHistory.acceptable("password: example",false));check(!ClipboardHistory.acceptable("رمز التحقق: ١٢٣٤",false));check(!ClipboardHistory.acceptable("-----BEGIN PRIVATE KEY-----",false));check(!ClipboardHistory.acceptable("test\u0000",false));check(ClipboardHistory.acceptable("مرحبا hello",false));
  ClipboardHistory h=new ClipboardHistory();check(h.nextExpiry(0,1000)==-1);check(h.save("hello",0==1,100,1000));check(h.nextExpiry(600,1000)==500);h.pin(h.list(600,1000).get(0).id,true,600,1000);check(h.nextExpiry(1200,1000)==-1);check(h.save("world",false,1200,1000));check(h.nextExpiry(2000,1000)==200);check(h.nextExpiry(2200,1000)==-1);check(h.list(2200,1000).size()==1);
  System.out.println("PASS: "+checks+" theme/emoji/privacy checks");
 }
}
