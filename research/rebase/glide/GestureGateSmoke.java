import com.zerokeyboard.ime.glide.GestureGate;
public class GestureGateSmoke {
 static int n;static void check(boolean x){if(!x)throw new AssertionError("case "+n);n++;}
 public static void main(String[] args){
  GestureGate g=new GestureGate();g.begin('a',0,0,0);check(g.tracking());check(!g.move('a',3,2,80,40));check(!g.claimed());
  check(!g.move('b',40,0,90,40));check(g.move('c',80,0,130,40));check(g.move('c',0,0,200,40));g.reset();check(!g.claimed()&&!g.tracking());
  g.begin(' ',0,0,0);check(!g.tracking());check(!g.move('a',200,0,100,40));
  g.begin('ض',0,0,0);check(!g.move('ص',40,0,15,40));check(!g.move('ث',80,0,25,40));check(g.move('ق',120,0,80,40));
  g.begin('A',0,0,0);check(!g.move('a',100,0,100,40));check(!g.move('b',100,0,200,40));check(!g.move('c',100,0,1300,40));
  g.begin('a',0,0,0);g.move('b',10,0,90,40);check(!g.move('c',20,0,100,40));g.reset();check(!g.move('d',200,0,200,40));
  System.out.println("PASS: "+n+" gesture admission checks");
 }
}
