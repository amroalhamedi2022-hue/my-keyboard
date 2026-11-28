import com.zerokeyboard.ime.glide.GeometricRanker;
import com.zerokeyboard.ime.glide.GeometricRanker.Point;
import com.zerokeyboard.ime.glide.GeometricRanker.Candidate;
import java.util.*;
public class RankerEquivalence {
 public static void main(String[] args){
  Random rng=new Random(20261128);GeometricRanker ranker=new GeometricRanker();int checked=0;
  for(String alphabet:Arrays.asList("abcdefghijklmnopqrstuvwxyz","ابتثجحخدذرزسشصضطظعغفقكلمنهوي")){
   Map<Integer,Point> keys=new HashMap<>();int[] cps=alphabet.codePoints().toArray();
   for(int i=0;i<cps.length;i++)keys.put(cps[i],new Point(i%10,i/10));
   Set<String> words=new LinkedHashSet<>();while(words.size()<600){StringBuilder w=new StringBuilder();int n=2+rng.nextInt(10);for(int j=0;j<n;j++)w.appendCodePoint(cps[rng.nextInt(cps.length)]);words.add(w.toString());}
   List<String> list=new ArrayList<>(words);
   for(int trial=0;trial<100;trial++){
    String target=list.get(rng.nextInt(list.size()));List<Point> trace=new ArrayList<>();
    target.codePoints().forEach(cp->{Point pt=keys.get(cp);trace.add(new Point(pt.x+(rng.nextDouble()-.5)*.6,pt.y+(rng.nextDouble()-.5)*.6));});
    List<Candidate> full=ranker.rank(trace,keys,list,list.size());
    for(int k:new int[]{1,3,10}){List<Candidate> limited=ranker.rank(trace,keys,list,k);assert limited.size()==Math.min(k,full.size());for(int i=0;i<limited.size();i++){assert limited.get(i).word.equals(full.get(i).word);assert Double.compare(limited.get(i).score,full.get(i).score)==0;}checked++;}
   }
  }
  System.out.println("PASS: "+checked+" deterministic top-K versus full-ranking cases (synthetic traces)");
 }
}
