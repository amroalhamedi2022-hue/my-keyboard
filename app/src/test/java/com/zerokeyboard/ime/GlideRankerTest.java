package com.zerokeyboard.ime;
import com.zerokeyboard.ime.glide.GeometricRanker;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class GlideRankerTest {
 @Test public void englishGeometry(){
  Map<Integer,GeometricRanker.Point> keys=new HashMap<>();
  keys.put((int)'c',new GeometricRanker.Point(0,0));keys.put((int)'a',new GeometricRanker.Point(1,1));keys.put((int)'t',new GeometricRanker.Point(2,0));
  assertEquals("cat",new GeometricRanker().rank(Arrays.asList(keys.get((int)'c'),keys.get((int)'a'),keys.get((int)'t')),keys,Arrays.asList("act","cat","unknown"),2).get(0).word);
 }
 @Test public void arabicGeometry(){
  Map<Integer,GeometricRanker.Point> keys=new HashMap<>();
  keys.put((int)'س',new GeometricRanker.Point(0,0));keys.put((int)'ل',new GeometricRanker.Point(1,1));keys.put((int)'ا',new GeometricRanker.Point(2,0));keys.put((int)'م',new GeometricRanker.Point(3,1));
  assertEquals("سلام",new GeometricRanker().rank(Arrays.asList(keys.get((int)'س'),keys.get((int)'ل'),keys.get((int)'ا'),keys.get((int)'م')),keys,Arrays.asList("مال","سلام"),2).get(0).word);
 }
 @Test public void rejectsEmptyTrace(){assertTrue(new GeometricRanker().rank(Collections.emptyList(),Collections.emptyMap(),Collections.emptyList(),3).isEmpty());}
}
