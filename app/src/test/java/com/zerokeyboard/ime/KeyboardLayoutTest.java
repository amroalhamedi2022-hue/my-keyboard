package com.zerokeyboard.ime;
import org.junit.Test;
import static org.junit.Assert.*;
public class KeyboardLayoutTest {
 @Test public void englishAndArabicAreSeparate(){
  assertTrue(KeyboardLayout.rows(KeyboardLayout.Language.EN,false,false,false).get(0).startsWith("q w"));
  assertTrue(KeyboardLayout.rows(KeyboardLayout.Language.AR,false,false,false).get(0).startsWith("ض ص"));
 }
 @Test public void numericOverridesAlphabet(){assertEquals("1 2 3",KeyboardLayout.rows(KeyboardLayout.Language.AR,true,true,true).get(0));}
 @Test public void shiftEnglish(){assertEquals("Q W E R T Y U I O P",KeyboardLayout.rows(KeyboardLayout.Language.EN,true,false,false).get(0));}
}
