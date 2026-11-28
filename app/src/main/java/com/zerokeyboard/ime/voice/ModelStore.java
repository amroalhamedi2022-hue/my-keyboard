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
 public synchronized ModelCatalog install(Uri uri)throws Exception{
  if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("model storage unavailable");
  File[] stale=dir.listFiles((ignored,name)->name.startsWith("import-")&&name.endsWith(".tmp"));
  if(stale!=null)for(File old:stale)if(!old.isDirectory())old.delete();
  File stage=File.createTempFile("import-",".tmp",dir);
  try{
   MessageDigest md=MessageDigest.getInstance("SHA-256");
   try(InputStream in=context.getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(stage)){
    if(in==null)throw new IOException("file unavailable");byte[] buf=new byte[65536];long total=0;int n;
    while((n=in.read(buf))!=-1){total+=n;if(total>MAX)throw new IOException("model too large");md.update(buf,0,n);out.write(buf,0,n);}out.getFD().sync();
   }
   ModelCatalog model=ModelCatalog.byHash(hex(md.digest()));
   if(model==null||stage.length()!=model.bytes)throw new IOException("unknown or corrupted model");
   try{Files.move(stage.toPath(),file(model).toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(AtomicMoveNotSupportedException unsupported){Files.move(stage.toPath(),file(model).toPath(),StandardCopyOption.REPLACE_EXISTING);}
   return model;
  }finally{stage.delete();}
 }
 public File file(ModelCatalog model){return new File(dir,model.filename);}
 public synchronized boolean installed(ModelCatalog model){File f=file(model);return Files.isRegularFile(f.toPath(),NOFOLLOW_LINKS)&&f.length()==model.bytes;}
 public synchronized File verified(ModelCatalog model)throws Exception{File f=file(model);if(!model.sha256.equals(digest(f,model.bytes)))throw new IOException("model missing or corrupted");return f;}
 public synchronized boolean delete(ModelCatalog model){File f=file(model);return !Files.exists(f.toPath(),NOFOLLOW_LINKS)||f.delete();}
}
