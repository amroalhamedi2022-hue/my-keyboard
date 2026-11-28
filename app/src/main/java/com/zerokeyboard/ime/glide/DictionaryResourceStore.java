package com.zerokeyboard.ime.glide;
import android.content.Context;
import android.net.Uri;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
/** Explicit local resource import; no network, no URI persistence and no typed-content access. */
public final class DictionaryResourceStore {
 private static final Object LOCK=new Object();
 private static long revision;
 private final Context context;private final File dir;
 private long seen=-1;
 private final Map<Boolean,List<String>> cache=new HashMap<>();
 public DictionaryResourceStore(Context c){context=c.getApplicationContext();dir=new File(context.getNoBackupFilesDir(),"dictionaries");}
 private AtomicFile file(boolean ar){return new AtomicFile(new File(dir,ar?"ar-frequency.txt":"en-frequency.txt"));}
 public int install(Uri uri,boolean ar)throws IOException{
  byte[] data;try(InputStream in=context.getContentResolver().openInputStream(uri)){data=FrequencyLexicon.read(in);}
  List<String> words=FrequencyLexicon.parse(data,ar);
  synchronized(LOCK){
   if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("resource directory unavailable");
   AtomicFile target=file(ar);FileOutputStream out=null;
   try{out=target.startWrite();out.write(data);target.finishWrite(out);out=null;}
   finally{if(out!=null)target.failWrite(out);}
   revision++;return words.size();
  }
 }
 public void remove(boolean ar){synchronized(LOCK){file(ar).delete();revision++;}}
 /** Called on background worker, never UI thread. */
 public List<String> words(boolean ar){
  synchronized(LOCK){
   if(seen!=revision){cache.clear();seen=revision;}
   List<String> existing=cache.get(ar);if(existing!=null)return existing;
   List<String> result;
   try(InputStream in=file(ar).openRead()){result=FrequencyLexicon.parse(FrequencyLexicon.read(in),ar);}
   catch(IOException|RuntimeException e){result=Collections.emptyList();}
   cache.put(ar,result);return result;
  }
 }
}
