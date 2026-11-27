package com.zerokeyboard.ime.glide;
import java.util.*;
/** Small original development seed, not a production dictionary or corpus. */
public final class SeedLexicon {
 private static final List<String> EN=Collections.unmodifiableList(Arrays.asList(
 ("hello hi welcome thanks thank please sorry yes no okay good great morning night day today tomorrow yesterday "+
 "time work home school study read write learn book phone tablet keyboard voice message text send receive "+
 "what when where why how who which this that these those here there with without from into about "+
 "have has had want need can could would should will make take give get know think see look come go "+
 "open close start stop help test words word english language arabic family friend friends people "+
 "happy nice fine love life world water food coffee tea dinner lunch breakfast cat dog house "+
 "quick brown fox jump over lazy typing example next back again really very more much after before "+
 "first second last new old right left long short fast slow now later never always something everything").split(" ")));
 private static final List<String> AR=Collections.unmodifiableList(Arrays.asList(
 ("السلام عليكم مرحبا أهلا اهلا صباح مساء الخير شكرا عفوا تمام حاضر نعم لا طيب منيح منيحة "+
 "كيف حالك شو ايش وين ليش متى مين هاد هادي هذا هذه هي هو انا أنا انت أنت انتي نحن احنا هم "+
 "اليوم بكرة مبارح أمس امس هلقيت هلق الآن الان بعد قبل هون هناك عندي عندك بدي بدك بدنا "+
 "ممكن لازم خلص يلا كمل شوف قول قولي احكي اكتب اقرأ اقرا اسمع تعلم دراسة كتاب مدرسة جامعة "+
 "بيت منزل شغل عمل وقت يوم ساعة دقيقة دقيقة ثانية تلفون هاتف رسالة كلام كلمة كلمات عربي "+
 "انجليزي لوحة مفاتيح صوت كتابة تطبيق برنامج جديد قديم سريع بطيء فتح سكر اغلق "+
 "روح تعال رايح جاي رجع رجوع راجع خلينا مع من في على الى إلى عن بدون بين حتى لما اذا إذا "+
 "لكن لأنه لانه يعني كثير كتير شوي قليل حلو جميل ممتاز رائع صح غلط تمام الحمد لله "+
 "سلام حب حياة أهل اهل عائلة صاحب صديق أصدقاء اصدقاء أكل اكل شرب ماء مي قهوة شاي "+
 "فطور غدا عشا اريد أريد عند معنا معك معاي يعطي الله رب خير إن شاء شاءالله").split(" ")));
 private SeedLexicon(){}
 public static List<String> words(boolean arabic){return arabic?AR:EN;}
}
