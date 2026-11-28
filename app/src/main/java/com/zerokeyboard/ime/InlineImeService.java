package com.zerokeyboard.ime;

import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InlineSuggestion;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.view.inputmethod.InlineSuggestionsResponse;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.inline.InlinePresentationSpec;
import androidx.autofill.inline.UiVersions;
import androidx.autofill.inline.v1.InlineSuggestionUi;
import java.util.Collections;
import java.util.List;

/** Hosts provider-rendered credential surfaces without inspecting their contents. */
abstract class InlineImeService extends InputMethodService {
    private static final int MAX_CHIPS = 6;
    private HorizontalScrollView strip;
    private LinearLayout chips;
    private long generation;
    private ZeroSettings preferences;

    @Override public void onCreate() {
        super.onCreate();
        preferences = new ZeroSettings(this);
    }

    final View wrapWithInlineStrip(View keyboard) {
        invalidateInlineSession();
        if (Build.VERSION.SDK_INT < 30) return keyboard;
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        strip = new HorizontalScrollView(this);
        strip.setHorizontalScrollBarEnabled(false);
        strip.setVisibility(View.GONE);
        chips = new LinearLayout(this);
        strip.addView(chips);
        wrapper.addView(strip, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapper.addView(keyboard, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return wrapper;
    }

    final void invalidateInlineSession() {
        generation++;
        if (chips != null) chips.removeAllViews();
        if (strip != null) strip.setVisibility(View.GONE);
    }

    @Override public InlineSuggestionsRequest onCreateInlineSuggestionsRequest(Bundle extras) {
        if (Build.VERSION.SDK_INT < 30 || !preferences.inlineAutofill() || extras == null
                || !UiVersions.getVersions(extras).contains(UiVersions.INLINE_UI_VERSION_1)) return null;
        Bundle style = UiVersions.newStylesBuilder()
                .addStyle(InlineSuggestionUi.newStyleBuilder().build()).build();
        float density = getResources().getDisplayMetrics().density;
        int height = Math.round(48 * density);
        int width = Math.max(height, Math.min(getResources().getDisplayMetrics().widthPixels,
                Math.round(360 * density)));
        InlinePresentationSpec spec = new InlinePresentationSpec.Builder(
                new Size(height, height), new Size(width, height)).setStyle(style).build();
        return new InlineSuggestionsRequest.Builder(Collections.singletonList(spec))
                .setMaxSuggestionCount(MAX_CHIPS).build();
    }

    @Override public boolean onInlineSuggestionsResponse(InlineSuggestionsResponse response) {
        invalidateInlineSession();
        if (Build.VERSION.SDK_INT < 30 || strip == null || !preferences.inlineAutofill()) return false;
        List<InlineSuggestion> suggestions = response.getInlineSuggestions();
        long token = generation;
        Size wrap = new Size(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        for (int i = 0; i < Math.min(suggestions.size(), MAX_CHIPS); i++) {
            FrameLayout slot = new FrameLayout(this);
            slot.setVisibility(View.GONE);
            chips.addView(slot);
            suggestions.get(i).inflate(this, wrap, getMainExecutor(), view -> {
                if (generation != token || view == null || !preferences.inlineAutofill()) return;
                slot.addView(view);
                slot.setVisibility(View.VISIBLE);
                strip.setVisibility(View.VISIBLE);
            });
        }
        return !suggestions.isEmpty();
    }
}
