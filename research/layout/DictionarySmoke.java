import com.zerokeyboard.ime.glide.FrequencyLexicon;
import java.nio.file.*;
public class DictionarySmoke {
 public static void main(String[] args)throws Exception{
  for(String lang:new String[]{"en","ar"}){
   byte[] data=Files.readAllBytes(Path.of("app/src/main/assets/dictionaries/"+lang+"-frequency.txt"));
   var words=FrequencyLexicon.parse(data,lang.equals("ar"));
   if(words.size()!=8000)throw new AssertionError(lang+" count "+words.size());
   if(!words.contains(lang.equals("ar")?"سلام":"hello"))throw new AssertionError(lang+" common word missing");
   System.out.println("PASS: "+lang+" bundled 8000-word frequency resource");
  }
 }
}
