import com.zerokeyboard.ime.LayoutProfile;
public class LayoutSmoke {
 public static void main(String[] args) {
  var p=LayoutProfile.resolve(360,false,false,LayoutProfile.Mode.SPLIT,100);
  assert p.mode==LayoutProfile.Mode.FULL&&p.gap==0;
  p=LayoutProfile.resolve(1100,true,true,LayoutProfile.Mode.SPLIT,100);
  assert p.mode==LayoutProfile.Mode.SPLIT&&p.gap==154&&p.keyHeight==48;
  p=LayoutProfile.resolve(360,false,false,LayoutProfile.Mode.LEFT,100);
  assert p.sideInset==80;
  p=LayoutProfile.resolve(1000,false,true,LayoutProfile.Mode.RIGHT,100);
  assert p.sideInset==300;
  assert LayoutProfile.resolve(360,false,false,LayoutProfile.Mode.FULL,999).keyHeight==60;
  assert LayoutProfile.resolve(360,true,false,LayoutProfile.Mode.FULL,1).keyHeight==32;
  assert LayoutProfile.localizeDigits("12.3",true).equals("١٢.٣");
  assert LayoutProfile.localizeDigits("12.3",false).equals("12.3");
  assert LayoutProfile.digits(true).equals("١ ٢ ٣ ٤ ٥ ٦ ٧ ٨ ٩ ٠");
  System.out.println("PASS: 9 layout assertions");
 }
}
