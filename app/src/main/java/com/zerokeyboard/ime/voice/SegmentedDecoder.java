package com.zerokeyboard.ime.voice;
import java.util.concurrent.*;
import java.util.function.Consumer;
/** One decoder thread, two pending chunks, explicit backpressure. Ownership transfers only on offer success. */
public final class SegmentedDecoder implements AutoCloseable {
 public interface Decoder{String decode(AudioSegments.Chunk chunk)throws Exception;}
 private final ArrayBlockingQueue<AudioSegments.Chunk> queue=new ArrayBlockingQueue<>(2);
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Future<String> result;
 private volatile boolean finished,cancelled,workerEnded;
 private volatile String completedText="";
 public SegmentedDecoder(Decoder decode,Consumer<String> partial){result=worker.submit(()->{
  StringBuilder text=new StringBuilder();
  try{while(!cancelled&&(!finished||!queue.isEmpty())){
    AudioSegments.Chunk chunk=queue.poll(50,TimeUnit.MILLISECONDS);if(chunk==null)continue;
    try{if(cancelled)break;String words=decode.decode(chunk);if(cancelled)break;if(words==null||words.trim().isEmpty())continue;
     String clean=words.trim();if(text.length()+clean.length()+1>16000)throw new IllegalStateException("dictation text limit");
     if(text.length()>0)text.append(' ');text.append(clean);completedText=text.toString();partial.accept(completedText);
    }finally{chunk.close();}
   }return cancelled?"":text.toString();
  }finally{endWorker();}
 });}
 public synchronized boolean offer(AudioSegments.Chunk chunk){return chunk!=null&&!finished&&!cancelled&&!workerEnded&&!result.isDone()&&queue.offer(chunk);}
 public boolean done(){return workerEnded||result.isDone();}
 public String completedText(){return completedText;}
 private synchronized void endWorker(){workerEnded=true;drain();}
 public synchronized void finish(){finished=true;}
 public String await(long timeout,TimeUnit unit)throws InterruptedException,ExecutionException,TimeoutException{return result.get(timeout,unit);}
 private void drain(){AudioSegments.Chunk chunk;while((chunk=queue.poll())!=null)chunk.close();}
 public synchronized void close(){cancelled=true;finished=true;completedText="";drain();worker.shutdownNow();}
}
