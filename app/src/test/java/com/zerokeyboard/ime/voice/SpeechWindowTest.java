package com.zerokeyboard.ime.voice;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpeechWindowTest {
    private void feed(SpeechWindow window, int count, int amplitude) {
        for (int i = 0; i < count; i++) window.accept((short)(i % 2 == 0 ? amplitude : -amplitude));
    }

    @Test public void silentInputAndIsolatedTapAreRejected() {
        SpeechWindow window = new SpeechWindow();
        feed(window, 16000, 0);
        feed(window, 320, 12000);
        feed(window, 32000, 0);
        assertFalse(window.detected());
    }

    @Test public void paddingAndEndpointUseAudioTime() {
        SpeechWindow window = new SpeechWindow();
        feed(window, 16000, 0);
        feed(window, 8000, 1000);
        feed(window, 19199, 0);
        assertTrue(window.detected());
        assertTrue(window.start() < 16000 && window.start() >= 12000);
        assertEquals(26560, window.end());
        assertFalse(window.ended(1200));
        feed(window, 1, 0);
        assertTrue(window.ended(1200));
    }
}
