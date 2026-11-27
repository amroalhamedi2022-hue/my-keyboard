package com.zerokeyboard.ime.voice;
import java.util.*;
public enum ModelCatalog {
 TINY("ggml-tiny-q5_1.bin","818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7"),
 BASE("ggml-base-q5_1.bin","422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898");
 public final String filename,sha256;
 ModelCatalog(String name,String hash){filename=name;sha256=hash;}
 public static ModelCatalog byHash(String hash){for(ModelCatalog model:values())if(model.sha256.equals(hash))return model;return null;}
}
