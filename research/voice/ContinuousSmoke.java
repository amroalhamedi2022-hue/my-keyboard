import com.zerokeyboard.ime.voice.AudioSegments;
import com.zerokeyboard.ime.voice.SegmentedDecoder;
import java.util.*;
import java.util.concurrent.*;
public class ContinuousSmoke {
 static int checks;
 static void check(boolean b){if(!b)throw new AssertionError("check "+checks);checks++;}
 static AudioSegments.Chunk speech(AudioSegments s){AudioSegments.Chunk chunk=null;for(int i=0;i<8000;i++){AudioSegments.Chunk c=s.accept((short)(i%2==0?3000:-3000));if(c!=null)throw new AssertionError();}for(int i=0;i<16000&&chunk==null;i++)chunk=s.accept((short)0);return chunk;}
 static boolean zero(AudioSegments.Chunk c){for(float x:c.pcm)if(x!=0f)return false;return true;}
 public static void main(String[] args)throws Exception{
  try(AudioSegments s=new AudioSegments(400)){
   for(int i=0;i<AudioSegments.MAX_SAMPLES;i++)if(s.accept((short)0)!=null)throw new AssertionError("silence emitted");check(s.finish()==null);
   AudioSegments.Chunk c=speech(s);check(c!=null&&c.pcm.length>=8000&&c.pcm.length<AudioSegments.MAX_SAMPLES);c.close();check(zero(c));check(s.finish()==null);
   AudioSegments.Chunk forced=null;for(int i=0;i<AudioSegments.MAX_SAMPLES;i++)forced=s.accept((short)(i%2==0?3000:-3000));check(forced!=null&&forced.pcm.length<=AudioSegments.MAX_SAMPLES);forced.close();
   List<String> partials=Collections.synchronizedList(new ArrayList<>());CountDownLatch entered=new CountDownLatch(1),resume=new CountDownLatch(1);java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
   try(SegmentedDecoder decoder=new SegmentedDecoder(chunk->{int n=calls.incrementAndGet();if(n==1){entered.countDown();resume.await();}return n==1?"بدي":n==2?"exam":"بكرة";},partials::add)){
    AudioSegments.Chunk a=speech(s),b=speech(s),d=speech(s),extra=speech(s);
    check(decoder.offer(a));check(entered.await(3,TimeUnit.SECONDS));check(decoder.offer(b));check(decoder.offer(d));check(!decoder.offer(extra));extra.close();
    decoder.finish();resume.countDown();check(decoder.await(5,TimeUnit.SECONDS).equals("بدي exam بكرة"));check(partials.equals(Arrays.asList("بدي","بدي exam","بدي exam بكرة")));check(zero(a)&&zero(b)&&zero(d)&&zero(extra));check(!decoder.offer(null));
   }
   AudioSegments.Chunk failed=speech(s);try(SegmentedDecoder decoder=new SegmentedDecoder(chunk->{throw new java.io.IOException("synthetic");},value->{})){
    check(decoder.offer(failed));decoder.finish();try{decoder.await(5,TimeUnit.SECONDS);throw new AssertionError();}catch(ExecutionException expected){check(expected.getCause() instanceof java.io.IOException);}check(zero(failed));
   }
  }
  System.out.println("PASS: "+checks+" segmentation/queue checks");
 }
}
