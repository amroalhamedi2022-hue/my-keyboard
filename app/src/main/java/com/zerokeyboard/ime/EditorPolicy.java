package com.zerokeyboard.ime;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
/** Fail closed for optional features when there is no known editor. Never reads editor contents. */
public final class EditorPolicy {
 public final boolean sensitive,numeric,allowPersonalization;
 private EditorPolicy(boolean s, boolean n, boolean p){sensitive=s;numeric=n;allowPersonalization=p;}
 public static EditorPolicy from(EditorInfo info){
  if(info==null) return new EditorPolicy(true,false,false);
  int c=info.inputType&InputType.TYPE_MASK_CLASS, v=info.inputType&InputType.TYPE_MASK_VARIATION;
  boolean numeric=c==InputType.TYPE_CLASS_NUMBER||c==InputType.TYPE_CLASS_PHONE||c==InputType.TYPE_CLASS_DATETIME;
  boolean sensitive=c==InputType.TYPE_NULL ||
    (c==InputType.TYPE_CLASS_TEXT&&(v==InputType.TYPE_TEXT_VARIATION_PASSWORD||v==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||v==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
    (c==InputType.TYPE_CLASS_NUMBER&&v==InputType.TYPE_NUMBER_VARIATION_PASSWORD);
  boolean noPersonalization=(info.imeOptions&EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)!=0 ||
    (c==InputType.TYPE_CLASS_TEXT&&(info.inputType&InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)!=0);
  // Numeric/phone contexts may be OTPs. Conservative default for future optional engines.
  return new EditorPolicy(sensitive||numeric,numeric,!sensitive&&!numeric&&!noPersonalization);
 }
}
