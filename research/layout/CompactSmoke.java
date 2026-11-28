import com.zerokeyboard.ime.CompactMetrics;
import com.zerokeyboard.ime.KeyboardLayout;
public class CompactSmoke{
 public static void main(String[] a){
  assert CompactMetrics.keyHeight(850,56,false,4,40,0)<=56;
  assert CompactMetrics.keyHeight(400,48,true,4,36,36)==32;
  assert CompactMetrics.toolbar(56)==40;
  assert CompactMetrics.toolbar(32)==32;
  assert KeyboardLayout.rows(KeyboardLayout.Language.AR,false,false,false).size()==3;
  assert KeyboardLayout.rows(KeyboardLayout.Language.EN,false,false,false).size()==3;
  assert KeyboardLayout.alternatives("ا").length==4;
  assert KeyboardLayout.alternatives(".").length>3;
  System.out.println("PASS: 8 compact layout checks");
 }
}
