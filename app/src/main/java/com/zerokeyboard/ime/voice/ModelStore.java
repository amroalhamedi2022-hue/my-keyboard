package com.zerokeyboard.ime.voice;
import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.Locale;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
/** Accept only exact catalogued weights. No arbitrary model parsing or network. */
public final class ModelStore {
 private static final long MAX=220L*1024*1024;
 private static final Object MODEL_WRITE_LOCK = new Object();
 private final Context context;
 private final File dir;
 public ModelStore(Context c){context=c.getApplicationContext();dir=new File(context.getNoBackupFilesDir(),"voice-models");}
 private static String hex(byte[] bytes){StringBuilder b=new StringBuilder();for(byte v:bytes)b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();}
 private static String digest(File file,long expected)throws Exception{
  Path path=file.toPath();
  if(!Files.isRegularFile(path,NOFOLLOW_LINKS)||Files.size(path)!=expected)throw new IOException("model type or size mismatch");
  MessageDigest md=MessageDigest.getInstance("SHA-256");
  try(InputStream in=new FileInputStream(file)){byte[] buf=new byte[65536];int n;long total=0;while((n=in.read(buf))!=-1){total+=n;if(total>MAX)throw new IOException("model too large");md.update(buf,0,n);}if(total!=expected)throw new IOException("model size changed");}return hex(md.digest());
 }
 public ModelCatalog install(Uri uri)throws Exception{
  synchronized (MODEL_WRITE_LOCK) {
   try(InputStream in=context.getContentResolver().openInputStream(uri)){
    if(in==null)throw new IOException("file unavailable");return installStream(in,null);
   }
  }
 }
 private ModelCatalog installStream(InputStream in,ModelCatalog expected)throws Exception{
  if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("model storage unavailable");
  File stage=File.createTempFile("import-",".tmp",dir);
  try{
   MessageDigest md=MessageDigest.getInstance("SHA-256");
   try(FileOutputStream out=new FileOutputStream(stage)){
    byte[] buf=new byte[65536];long total=0;int n;
    while((n=in.read(buf))!=-1){total+=n;if(total>MAX)throw new IOException("model too large");md.update(buf,0,n);out.write(buf,0,n);}out.getFD().sync();
   }
   ModelCatalog model=ModelCatalog.byHash(hex(md.digest()));
   if(model==null||(expected!=null&&model!=expected)||stage.length()!=model.bytes)throw new IOException("unknown or corrupted model");
   try{Files.move(stage.toPath(),file(model).toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(AtomicMoveNotSupportedException unsupported){Files.move(stage.toPath(),file(model).toPath(),StandardCopyOption.REPLACE_EXISTING);}
   return model;
  }finally{stage.delete();}
 }
 public File file(ModelCatalog model){return new File(dir,model.filename);}
 public boolean installed(ModelCatalog model){File f=file(model);return Files.isRegularFile(f.toPath(),NOFOLLOW_LINKS)&&f.length()==model.bytes;}
 private static final String BUNDLE="zero-voice-models";
 public boolean bundled(ModelCatalog model){
  if(model!=ModelCatalog.TINY)return false;
  try{String[] names=context.getAssets().list(BUNDLE);if(names!=null)for(String name:names)if(model.filename.equals(name))return true;}catch(IOException ignored){}
  return false;
 }
 public boolean available(ModelCatalog model){return installed(model)||bundled(model);}
 /** Called only on the voice worker. The packaged model is checked before atomic installation. */
 public File verified(ModelCatalog model)throws Exception{
  synchronized (MODEL_WRITE_LOCK) {
   File f=file(model);
   try{if(model.sha256.equals(digest(f,model.bytes)))return f;}catch(IOException ignored){}
   if(!bundled(model))throw new IOException("model missing or corrupted");
   try(InputStream in=context.getAssets().open(BUNDLE+"/"+model.filename)){installStream(in,model);}
   return f;
  }
 }
 public boolean delete(ModelCatalog model){
  synchronized (MODEL_WRITE_LOCK) {
   File f=file(model);
   return !Files.exists(f.toPath(),NOFOLLOW_LINKS)||f.delete();
  }
 }
}
