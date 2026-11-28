package com.zerokeyboard.ime.glide;
import java.util.*;
import java.util.function.BooleanSupplier;
/** Experimental IME geometric ranker. Coordinates use consistent keyboard-width units. */
public final class GeometricRanker {
 private static final int SAMPLES=32;
 public static final class Point {
  public final double x,y;
  public Point(double x,double y){if(!Double.isFinite(x)||!Double.isFinite(y))throw new IllegalArgumentException("non-finite point");this.x=x;this.y=y;}
 }
 public static final class Candidate {
  public final String word; public final double score;
  Candidate(String w,double s){word=w;score=s;}
 }
 private static final class Template {
  final String word; final List<Point> sampled; final Point first,last; final double length,prior;
  Template(String word,List<Point> raw,double prior){this.word=word;this.prior=prior;sampled=sample(raw);first=raw.get(0);last=raw.get(raw.size()-1);length=GeometricRanker.length(raw);}
 }
 private Map<Integer,Point> cachedKeys=Collections.emptyMap();
 private List<String> cachedWords=Collections.emptyList();
 private List<Template> cachedTemplates=Collections.emptyList();
 private static double d(Point a,Point b){return Math.hypot(a.x-b.x,a.y-b.y);}
 private static double length(List<Point> p){double n=0;for(int i=1;i<p.size();i++)n+=d(p.get(i-1),p.get(i));return n;}
 private static List<Point> sample(List<Point> p){
  double total=length(p); List<Point> out=new ArrayList<>();
  if(total==0){for(int i=0;i<SAMPLES;i++)out.add(p.get(0));return out;}
  int j=1;double consumed=0;
  for(int i=0;i<SAMPLES;i++){
   double target=total*i/(SAMPLES-1);
   while(j<p.size()-1&&consumed+d(p.get(j-1),p.get(j))<target){consumed+=d(p.get(j-1),p.get(j));j++;}
   Point a=p.get(j-1),b=p.get(j);double segment=d(a,b);
   double f=segment==0?0:Math.max(0,Math.min(1,(target-consumed)/segment));
   out.add(new Point(a.x+(b.x-a.x)*f,a.y+(b.y-a.y)*f));
  }return out;
 }
 private static int normalized(int cp){
  cp=Character.toLowerCase(cp);
  return cp=='أ'||cp=='إ'||cp=='آ'? 'ا':cp;
 }
 private static Point key(Map<Integer,Point> keys,int cp){
  Point value=keys.get(cp);
  return value!=null?value:keys.get(normalized(cp));
 }
 private static boolean sameKeys(Map<Integer,Point> first,Map<Integer,Point> second){
  if(first.size()!=second.size())return false;
  for(Map.Entry<Integer,Point> entry:first.entrySet()){
   Point other=second.get(entry.getKey());Point point=entry.getValue();
   if(other==null||Double.doubleToLongBits(point.x)!=Double.doubleToLongBits(other.x)||Double.doubleToLongBits(point.y)!=Double.doubleToLongBits(other.y))return false;
  }return true;
 }
 private boolean prepare(Map<Integer,Point> keys,Collection<String> words,BooleanSupplier cancelled){
  List<String> wordList=new ArrayList<>(words);
  if(wordList.equals(cachedWords)&&sameKeys(keys,cachedKeys))return true;
  Map<Integer,Point> keyCopy=new HashMap<>(keys);List<Template> templates=new ArrayList<>();
  int index=0;for(String word:wordList){int position=index++;
   if(cancelled.getAsBoolean())return false;
   if(word.isEmpty())continue;List<Point> raw=new ArrayList<>();
   for(int i=0;i<word.length();){int cp=word.codePointAt(i);i+=Character.charCount(cp);Point point=key(keyCopy,cp);if(point==null){raw.clear();break;}raw.add(point);}
   if(raw.size()>=2&&length(raw)>0)templates.add(new Template(word,raw,0.12*Math.log1p(position)/Math.log1p(Math.max(1,wordList.size()))));
  }
  cachedKeys=keyCopy;cachedWords=wordList;cachedTemplates=templates;
  return true;
 }
 public List<Candidate> rank(List<Point> trace,Map<Integer,Point> keys,Collection<String> words,int topK){
  return rank(trace,keys,words,topK,()->false);
 }
 public synchronized List<Candidate> rank(List<Point> trace,Map<Integer,Point> keys,Collection<String> words,int topK,BooleanSupplier cancelled){
  if(trace.size()<2||topK<1||length(trace)==0||cancelled.getAsBoolean()||!prepare(keys,words,cancelled))return Collections.emptyList();
  List<Point> actual=sample(trace);int limit=Math.min(topK,cachedTemplates.size());if(limit==0)return Collections.emptyList();
  Comparator<Candidate> order=Comparator.comparingDouble((Candidate c)->c.score).thenComparing(c->c.word);
  PriorityQueue<Candidate> best=new PriorityQueue<>(limit,order.reversed());double traceLength=length(trace);
  for(Template template:cachedTemplates){
   if(cancelled.getAsBoolean())return Collections.emptyList();
   double lower=template.prior+0.4*(d(trace.get(0),template.first)+d(trace.get(trace.size()-1),template.last))+0.08*Math.abs(traceLength-template.length);
   if(best.size()==limit&&lower>best.peek().score)continue;
   double error=0;for(int i=0;i<SAMPLES;i++)error+=d(actual.get(i),template.sampled.get(i));
   Candidate candidate=new Candidate(template.word,error/SAMPLES+lower);
   if(best.size()<limit)best.add(candidate);else if(order.compare(candidate,best.peek())<0){best.poll();best.add(candidate);}
  }
  if(cancelled.getAsBoolean())return Collections.emptyList();
  List<Candidate> ranked=new ArrayList<>(best);ranked.sort(order);return ranked;
 }
}
