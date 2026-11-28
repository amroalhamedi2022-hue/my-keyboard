import com.zerokeyboard.ime.voice.NativeWhisper;
import com.zerokeyboard.ime.voice.VoiceOptions;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;
import javax.sound.sampled.AudioSystem;

/** Host smoke test of the production JNI bridge with the public Whisper English sample. */
public final class NativeInferenceSmoke {
    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !NativeWhisper.available()) {
            throw new IllegalStateException("Expected JNI library, verified model path and WAV path");
        }
        float[] pcm;
        try (var audio = AudioSystem.getAudioInputStream(new File(args[1]))) {
            var format = audio.getFormat();
            if (format.getSampleRate() != 16000 || format.getChannels() != 1
                    || format.getSampleSizeInBits() != 16 || format.isBigEndian()) {
                throw new IllegalArgumentException("Expected mono signed PCM16 WAV at 16 kHz");
            }
            var samples = ByteBuffer.wrap(audio.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
            pcm = new float[samples.remaining() / Short.BYTES];
            for (int i = 0; i < pcm.length; i++) pcm[i] = samples.getShort() / 32768f;
        }
        var options = new VoiceOptions(0, 4, true, 1200);
        long session = NativeWhisper.newSession();
        try {
            long start = System.nanoTime();
            String transcript = NativeWhisper.transcribe(args[0], pcm, pcm.length, session, options);
            String normalized = transcript.toLowerCase(Locale.ROOT).replaceAll("[^a-z]+", " ");
            if (!normalized.contains("what your country can do for you")) {
                throw new AssertionError("Expected English phrase absent from JNI transcription");
            }
            System.out.printf(Locale.ROOT, "PASS: production JNI decoded public English audio in %.3f s%n",
                    (System.nanoTime() - start) / 1_000_000_000d);
            NativeWhisper.cancel(session);
            try {
                NativeWhisper.transcribe(args[0], pcm, pcm.length, session, options);
                throw new AssertionError("Cancelled session accepted a second decode");
            } catch (IllegalStateException rejected) {
                System.out.println("PASS: cancelled JNI session rejected");
            }
        } finally {
            NativeWhisper.release(session);
            Arrays.fill(pcm, 0);
        }
    }
}
