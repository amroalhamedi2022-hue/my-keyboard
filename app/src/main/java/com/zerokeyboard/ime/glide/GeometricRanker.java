package com.zerokeyboard.ime.glide;
import java.util.*;
/** Research baseline, not wired into IME. Coordinates use consistent keyboard-width units. */
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
 public List<Candidate> rank(List<Point> trace,Map<Integer,Point> keys,Collection<String> words,int topK){
  if(trace.size()<2||topK<1||length(trace)==0)return Collections.emptyList();
  List<Point> actual=sample(trace);
  List<Candidate> ranked=new ArrayList<>();
  for(String word:words){
   if(word.isEmpty())continue;
   List<Point> template=new ArrayList<>();
   for(int i=0;i<word.length();){
    int cp=word.codePointAt(i);i+=Character.charCount(cp);Point key=keys.get(cp);
    if(key==null){template.clear();break;}template.add(key);
   }
   if(template.size()<2||length(template)==0)continue;
   List<Point> expected=sample(template);double error=0;
   for(int i=0;i<SAMPLES;i++)error+=d(actual.get(i),expected.get(i));
   double score=error/SAMPLES+0.4*(d(trace.get(0),template.get(0))+d(trace.get(trace.size()-1),template.get(template.size()-1)))+0.08*Math.abs(length(trace)-length(template));
   ranked.add(new Candidate(word,score));
  }
  ranked.sort(Comparator.comparingDouble((Candidate c)->c.score).thenComparing(c->c.word));
  return new ArrayList<>(ranked.subList(0,Math.min(topK,ranked.size())));
 }
}
