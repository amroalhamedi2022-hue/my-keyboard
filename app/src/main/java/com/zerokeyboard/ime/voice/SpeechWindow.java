package com.zerokeyboard.ime.voice;

/** Lightweight endpoint detector for 16 kHz mono PCM. It does not retain audio. */
final class SpeechWindow {
    private static final int FRAME = 320;
    private static final int MARGIN = 8 * FRAME;
    private int inFrame, total, recent, first = -1, last;
    private double energy;

    void accept(short sample) {
        double value = sample / 32768.0;
        energy += value * value;
        total++;
        if (++inFrame < FRAME) return;
        boolean active = energy / FRAME >= 0.0025 * 0.0025;
        recent = ((recent << 1) | (active ? 1 : 0)) & 31;
        if (active && Integer.bitCount(recent) >= 3) {
            if (first < 0) first = Math.max(0, total - 5 * FRAME - MARGIN);
            last = total;
        }
        energy = 0;
        inFrame = 0;
    }

    boolean detected() { return first >= 0; }
    boolean ended(int silenceMillis) { return detected() && total - last >= 16L * silenceMillis; }
    int start() { return first; }
    int end() { return Math.min(total, last + MARGIN); }
}
