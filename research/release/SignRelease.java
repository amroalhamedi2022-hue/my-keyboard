import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
/** Offline owner-side signing. No key/password is passed to CI, Git or command-line arguments. */
public final class SignRelease {
 private static final String EXPECTED="a0c2454c1ec3b80e0e20def4c94d9e31e8dc52368a589cf7299e97c6ed087afe";
 static String digest(X509Certificate c)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(c.getEncoded()));}
 public static void main(String[] args)throws Exception{
  if(args.length!=4)throw new IllegalArgumentException("unsigned.apk signed.apk owner.p12 password-file");
  Path input=Path.of(args[0]),output=Path.of(args[1]);if(!Files.isRegularFile(input)||Files.exists(output)||input.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize()))throw new IOException("invalid input or output");
  char[] password=Files.readString(Path.of(args[3])).strip().toCharArray();
  try{
   KeyStore store=KeyStore.getInstance("PKCS12");try(InputStream in=Files.newInputStream(Path.of(args[2]))){store.load(in,password);}
   PrivateKey key=(PrivateKey)store.getKey("zero-keyboard",password);X509Certificate cert=(X509Certificate)store.getCertificate("zero-keyboard");
   if(key==null||cert==null||!EXPECTED.equals(digest(cert)))throw new SecurityException("release certificate mismatch");
   ApkSigner.SignerConfig signer=new ApkSigner.SignerConfig.Builder("ZeroKeyboard",key,Collections.singletonList(cert)).build();
   try{
    new ApkSigner.Builder(Collections.singletonList(signer)).setInputApk(input.toFile()).setOutputApk(output.toFile()).setMinSdkVersion(29).setV1SigningEnabled(false).setV2SigningEnabled(true).setV3SigningEnabled(true).build().sign();
    ApkVerifier.Result result=new ApkVerifier.Builder(output.toFile()).build().verify();
    if(!result.isVerified()||(!result.isVerifiedUsingV2Scheme()&&!result.isVerifiedUsingV3Scheme())||result.getSignerCertificates().size()!=1||!EXPECTED.equals(digest(result.getSignerCertificates().get(0))))throw new SecurityException("APK verification failed: "+result.getErrors());
    System.out.println("Verified release certificate SHA-256: "+EXPECTED);
   }catch(Exception e){Files.deleteIfExists(output);throw e;}
  }finally{Arrays.fill(password,'\0');}
 }
}
