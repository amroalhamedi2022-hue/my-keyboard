package com.zerokeyboard.ime.tools;
import java.util.*;
/** Session-only, explicitly saved plain text. No automatic clipboard listener. */
public final class ClipboardHistory {
 public static final int MAX_TEXT=2048, MAX_ITEMS=12;
 public static final class Entry {
  public final long id; public final String text; public final long savedAt; public final boolean pinned;
  Entry(long id,String text,long time,boolean pinned){this.id=id;this.text=text;this.savedAt=time;this.pinned=pinned;}
 }
 private final ArrayList<Entry> entries=new ArrayList<>();private long nextId;
 public static boolean acceptable(String text,boolean sensitive){
  if(sensitive||text==null||text.isEmpty()||text.length()>MAX_TEXT||text.trim().isEmpty())return false;
  // Conservative rejection of numeric-only tokens, OTP-like and card-like digit runs.
  int digits=0,run=0;boolean letter=false;
  for(int i=0;i<text.length();){int cp=text.codePointAt(i);i+=Character.charCount(cp);
   if(Character.isDigit(cp)){digits++;run++;if(run>=6)return false;}
   else if(!Character.isWhitespace(cp)&&cp!='-'&&cp!='('&&cp!=')'&&cp!='+')run=0;
   if(Character.isLetter(cp))letter=true;
  }
  return letter||digits==0;
 }
 public void prune(long now,long ttl){entries.removeIf(e->!e.pinned&&(now<e.savedAt||now-e.savedAt>=ttl));}
 public boolean save(String text,boolean sensitive,long now,long ttl){
  prune(now,ttl);if(!acceptable(text,sensitive))return false;
  for(int i=0;i<entries.size();i++)if(entries.get(i).text.equals(text)){Entry old=entries.remove(i);entries.add(0,new Entry(old.id,text,now,old.pinned));return true;}
  if(entries.size()>=MAX_ITEMS){int i=entries.size()-1;while(i>=0&&entries.get(i).pinned)i--;if(i<0)return false;entries.remove(i);}
  entries.add(0,new Entry(++nextId,text,now,false));return true;
 }
 public List<Entry> list(long now,long ttl){prune(now,ttl);return Collections.unmodifiableList(new ArrayList<>(entries));}
 public void pin(long id,boolean pinned,long now,long ttl){prune(now,ttl);for(int i=0;i<entries.size();i++){Entry e=entries.get(i);if(e.id==id){entries.set(i,new Entry(e.id,e.text,e.savedAt,pinned));break;}}prune(now,ttl);}
 public void remove(long id){entries.removeIf(e->e.id==id);}
 public void clear(){entries.clear();}
}
