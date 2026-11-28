package com.zerokeyboard.ime.tools;
import java.util.*;
import java.text.Normalizer;
/** Small original bilingual search catalog. No imported assets or network. */
public final class EmojiCatalog {
 private EmojiCatalog(){}
 private static final String[][] GROUPS={
  {"😀","😃","😄","😁","😆","😅","😂","🙂","🙃","😉","😊","😍","🥰","😘","😎","🤔","😴","😭","😢","😡","😮","🤩","🥳","😇"},
  {"👍","👎","👏","🙌","🙏","🤝","👋","✌️","👌","🤲","💪","🫶","❤️","💙","💚","💛","🤍","🖤","💜","💔","✨","🔥","⭐","💯"},
  {"🐱","🐶","🐦","🦋","🐝","🌹","🌷","🌻","🌿","🌳","🌴","🌙","☀️","🌧️","🌈","🌊","🍎","🍌","🍇","🍉","☕","🍵","🍕","🍰"},
  {"📱","💻","⌚","📚","✏️","📝","📌","🔑","🔒","💡","📷","🎧","🚗","✈️","🏠","🏫","⚽","🎮","🎉","🎁","✅","❌","⚠️","❓"}
 };
 private static final String[][] ALIASES={
  {"happy smile سعيد ابتسامة","happy smile فرح ابتسامة","grin joy ضحك فرح","grin teeth ضحكة أسنان","laugh ضحك","laugh sweat ضحك عرق","laugh tears crying ضحك دموع","smile ابتسامة","upside down مقلوب","wink غمزة","blush smile خجل ابتسامة","love heart حب قلب","love hearts حب قلوب","kiss قبلة","cool sunglasses نظارة شمس","think thinking تفكير","sleep tired نوم تعب","cry tears بكاء دموع","sad tear حزن دمعة","angry غضب","surprise shocked دهشة","star eyes stars نجوم إعجاب","party celebrate احتفال","angel halo ملاك"},
  {"thumb up like good إعجاب ممتاز موافق","thumb down dislike رفض","clap applause تصفيق","raise hands celebrate فرح احتفال","pray prayer thanks دعاء شكر","handshake اتفاق مصافحة","wave hello bye سلام وداع","peace victory نصر سلام","ok okay تمام","pray hands دعاء","strong muscle قوة عضلات","heart hands حب قلب","red heart love قلب أحمر حب","blue heart قلب أزرق","green heart قلب أخضر","yellow heart قلب أصفر","white heart قلب أبيض","black heart قلب أسود","purple heart قلب بنفسجي","broken heart قلب مكسور حزن","sparkle shine لمعان","fire hot نار","star نجمة","hundred perfect مئة ممتاز"},
  {"cat قطة قط","dog كلب","bird عصفور طائر","butterfly فراشة","bee نحلة","rose وردة","tulip زهرة","sunflower عباد الشمس","leaf leaves ورق نبات","tree شجرة","palm نخلة","moon قمر","sun شمس","rain مطر","rainbow قوس قزح","wave sea موج بحر","apple تفاح","banana موز","grapes عنب","watermelon بطيخ","coffee قهوة","tea شاي","pizza بيتزا","cake كيك حلوى"},
  {"phone mobile هاتف جوال","laptop computer حاسوب كمبيوتر","watch clock ساعة","books book كتب كتاب","pencil قلم","note writing ملاحظة كتابة","pin دبوس","key مفتاح","lock قفل","light idea مصباح فكرة","camera كاميرا","headphones سماعات","car سيارة","plane airplane طائرة سفر","home house منزل بيت","school مدرسة","football soccer كرة قدم","game controller لعبة","party celebrate احتفال","gift present هدية","check yes صح صحيح","cross no خطأ غلط","warning caution تحذير","question سؤال"}
 };
 private static final Set<String> TONE_BASES=new HashSet<>(Arrays.asList("👍","👎","👏","🙌","🙏","👋","✌️","👌","🤲","💪","🫶"));
 static{for(int i=0;i<GROUPS.length;i++)if(GROUPS[i].length!=ALIASES[i].length)throw new IllegalStateException("emoji metadata mismatch");}
 public static String[] group(int n){return GROUPS[Math.max(0,Math.min(GROUPS.length-1,n))].clone();}
 public static int count(){return GROUPS.length;}
 private static String normalized(String s){return Normalizer.normalize(s,Normalizer.Form.NFC).toLowerCase(Locale.ROOT).replaceAll("[\u064B-\u065F\u0670]","").replace('أ','ا').replace('إ','ا').replace('آ','ا').trim();}
 public static List<String> search(String query,int max){
  if(query==null||query.length()>64||max<1)return Collections.emptyList();String q=normalized(query);if(q.isEmpty())return Collections.emptyList();
  String[] terms=q.split("\\s+");List<String> out=new ArrayList<>();
  for(int i=0;i<GROUPS.length;i++)for(int j=0;j<GROUPS[i].length;j++){
   String text=normalized(ALIASES[i][j])+" "+GROUPS[i][j];boolean match=true;for(String term:terms)if(!text.contains(term)){match=false;break;}
   if(match){out.add(GROUPS[i][j]);if(out.size()>=Math.min(96,max))return Collections.unmodifiableList(out);}
  }return Collections.unmodifiableList(out);
 }
 public static String skinTone(String emoji,int tone){if(tone<1||tone>5||!TONE_BASES.contains(emoji))return emoji;return emoji.replace("\uFE0F","")+new String(Character.toChars(0x1F3FA+tone));}
}
