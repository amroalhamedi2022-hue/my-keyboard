import com.zerokeyboard.ime.KeyboardLayout;
import com.zerokeyboard.ime.glide.GeometricRanker;
import java.util.*;
public class Smoke {
 public static void main(String[] args) {
  assert KeyboardLayout.rows(KeyboardLayout.Language.EN,false,false,false).get(0).startsWith("q w");
  assert KeyboardLayout.rows(KeyboardLayout.Language.AR,false,false,false).get(0).startsWith("ض ص");
  assert KeyboardLayout.rows(KeyboardLayout.Language.AR,true,true,true).get(0).equals("1 2 3");
  assert KeyboardLayout.rows(KeyboardLayout.Language.EN,true,false,false).get(0).equals("Q W E R T Y U I O P");
  var ranker=new GeometricRanker();
  for (String word: Arrays.asList("cat","سلام")) {
   var keys=new HashMap<Integer,GeometricRanker.Point>();
   var trace=new ArrayList<GeometricRanker.Point>();
   int i=0;
   for(int cp:word.codePoints().toArray()){var point=new GeometricRanker.Point(i,i%2);keys.put(cp,point);trace.add(point);i++;}
   assert ranker.rank(trace,keys,Arrays.asList(new StringBuilder(word).reverse().toString(),word,"unknown"),2).get(0).word.equals(word);
  }
  assert ranker.rank(Collections.emptyList(),Collections.emptyMap(),Collections.emptyList(),3).isEmpty();
  System.out.println("PASS: 7 JVM smoke assertions");
 }
}
