package com.zerokeyboard.ime.voice;
import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.Locale;
/** Accept only exact catalogued weights. No arbitrary model parsing or network. */
public final class ModelStore {
 private static final long MAX=220L*1024*1024;
 private final Context context;
 private final File dir;
 public ModelStore(Context c){context=c.getApplicationContext();dir=new File(context.getNoBackupFilesDir(),"voice-models");}
 private static String hex(byte[] bytes){StringBuilder b=new StringBuilder();for(byte v:bytes)b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();}
 private static String digest(File file)throws Exception{
  MessageDigest md=MessageDigest.getInstance("SHA-256");
  try(InputStream in=new FileInputStream(file)){byte[] buf=new byte[65536];int n;long total=0;while((n=in.read(buf))!=-1){total+=n;if(total>MAX)throw new IOException("model too large");md.update(buf,0,n);}}return hex(md.digest());
 }
 public ModelCatalog install(Uri uri)throws Exception{
  if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("model storage unavailable");
  File stage=File.createTempFile("import-",".tmp",dir);
  try{
   MessageDigest md=MessageDigest.getInstance("SHA-256");
   try(InputStream in=context.getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(stage)){
    if(in==null)throw new IOException("file unavailable");byte[] buf=new byte[65536];long total=0;int n;
    while((n=in.read(buf))!=-1){total+=n;if(total>MAX)throw new IOException("model too large");md.update(buf,0,n);out.write(buf,0,n);}out.getFD().sync();
   }
   ModelCatalog model=ModelCatalog.byHash(hex(md.digest()));
   if(model==null)throw new IOException("unknown or corrupted model");
   Files.move(stage.toPath(),file(model).toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
   return model;
  }finally{stage.delete();}
 }
 public File file(ModelCatalog model){return new File(dir,model.filename);}
 public boolean installed(ModelCatalog model){return file(model).isFile();}
 public File verified(ModelCatalog model)throws Exception{File f=file(model);if(!f.isFile()||!model.sha256.equals(digest(f)))throw new IOException("model missing or corrupted");return f;}
 public boolean delete(ModelCatalog model){File f=file(model);return !f.exists()||f.delete();}
}
