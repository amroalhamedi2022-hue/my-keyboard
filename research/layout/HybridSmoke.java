import com.zerokeyboard.ime.SpaceGesture;
public class HybridSmoke {
 static int checks;static void check(boolean x){if(!x)throw new AssertionError("case "+checks);checks++;}
 public static void main(String[] args){
  var g=new SpaceGesture(SpaceGesture.Mode.CURSOR,12,14);g.begin(0);check(g.move(8)==0);check(g.end(8,true)==SpaceGesture.End.SPACE);
  g.begin(0);check(g.move(42)==3);check(g.move(14)==-2);check(g.end(14,true)==SpaceGesture.End.NONE);
  g.begin(0);check(g.move(1000)==8);g.cancel();check(g.end(0,true)==SpaceGesture.End.NONE);
  g=new SpaceGesture(SpaceGesture.Mode.LANGUAGE,32,14);g.begin(100);check(g.move(30)==0);check(g.end(30,true)==SpaceGesture.End.SWITCH);g.begin(100);g.move(30);check(g.end(100,true)==SpaceGesture.End.NONE);g.begin(100);check(g.end(105,true)==SpaceGesture.End.SPACE);
  g=new SpaceGesture(SpaceGesture.Mode.OFF,12,14);g.begin(0);check(g.move(100)==0);check(g.end(100,true)==SpaceGesture.End.NONE);g.begin(0);check(g.end(0,false)==SpaceGesture.End.NONE);check(g.end(0,true)==SpaceGesture.End.NONE);
  System.out.println("PASS: "+checks+" space gesture checks");
 }
}
