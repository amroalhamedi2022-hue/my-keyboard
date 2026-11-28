import com.zerokeyboard.ime.voice.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public class RecoverySmoke {
 static AudioSegments.Chunk chunk(){try(AudioSegments s=new AudioSegments(1200)){for(int i=0;i<5000;i++)s.accept((short)(i%2==0?10000:-10000));var c=s.finish();if(c==null)throw new AssertionError("fixture");return c;}}
 static void check(boolean x){if(!x)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
 for(int i=0;i<100;i++){AtomicInteger calls=new AtomicInteger();try(SegmentedDecoder d=new SegmentedDecoder(c->{if(calls.incrementAndGet()==1)return "first";throw new IllegalStateException("synthetic failure");},s->{})){var a=chunk();var b=chunk();check(d.offer(a));check(d.offer(b));d.finish();try{d.await(2,TimeUnit.SECONDS);throw new AssertionError("expected error");}catch(ExecutionException expected){}check(d.done());check(d.completedText().equals("first"));var late=chunk();check(!d.offer(late));late.close();check(a.pcm[0]==0f&&b.pcm[0]==0f);}}
 System.out.println("PASS: 100 interrupted decode sessions, prior text preserved, late chunk rejected, audio wiped");
 }
}
