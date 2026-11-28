import com.zerokeyboard.ime.glide.FrequencyLexicon;
import com.zerokeyboard.ime.glide.GeometricRanker;
import com.zerokeyboard.ime.glide.SeedLexicon;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Replays observed trajectories against the experimental decoder and bundled dictionary. */
public final class HumanTraceBenchmark {
    private record Trace(String word, List<GeometricRanker.Point> points) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("sample-directory frequency-file");
        Path input = Path.of(args[0]);
        Map<Integer, GeometricRanker.Point> centers = new HashMap<>();
        for (String line : Files.readAllLines(input.resolve("centers.tsv"))) {
            String[] row = line.split("\t");
            centers.put((int) row[0].charAt(0), point(row[1], row[2]));
        }
        List<Trace> traces = new ArrayList<>();
        for (String line : Files.readAllLines(input.resolve("traces.tsv"))) {
            String[] row = line.split("\t", 2);
            List<GeometricRanker.Point> points = new ArrayList<>();
            for (String pair : row[1].split(" ")) {
                String[] xy = pair.split(",");
                points.add(point(xy[0], xy[1]));
            }
            traces.add(new Trace(row[0], points));
        }
        if (centers.size() != 26 || traces.isEmpty()) throw new IllegalArgumentException("missing benchmark data");
        List<String> words = FrequencyLexicon.merge(SeedLexicon.words(false), List.of(),
                FrequencyLexicon.parse(Files.readAllBytes(Path.of(args[1])), false));
        Set<String> vocabulary = new HashSet<>(words);
        GeometricRanker decoder = new GeometricRanker();
        for (int pass = 0; pass < 3; pass++) {
            for (Trace trace : traces) decoder.rank(trace.points(), centers, words, 3);
        }
        int covered = 0, top1 = 0, top3 = 0, emitted = 0, correctEmissions = 0;
        List<Long> elapsed = new ArrayList<>();
        for (Trace trace : traces) {
            if (vocabulary.contains(trace.word())) covered++;
            long started = System.nanoTime();
            var candidates = decoder.rank(trace.points(), centers, words, 3);
            elapsed.add(System.nanoTime() - started);
            if (candidates.isEmpty()) continue;
            var first = candidates.get(0);
            boolean accurate = first.word.equals(trace.word());
            if (accurate) top1++;
            if (candidates.stream().anyMatch(candidate -> candidate.word.equals(trace.word()))) top3++;
            if (first.score <= 2.0) {
                emitted++;
                if (accurate) correctEmissions++;
            }
        }
        elapsed.sort(Long::compareTo);
        double p50 = elapsed.get((int) Math.ceil(elapsed.size() * 0.50) - 1) / 1_000_000.0;
        double p95 = elapsed.get((int) Math.ceil(elapsed.size() * 0.95) - 1) / 1_000_000.0;
        System.out.printf(Locale.ROOT,
                "{\"traces\": %d, \"vocabulary\": %d, \"covered\": %d, \"top1\": %d, \"top3\": %d, "
                + "\"emitted\": %d, \"correct_emissions\": %d, \"rank_p50_ms\": %.3f, \"rank_p95_ms\": %.3f}%n",
                traces.size(), words.size(), covered, top1, top3, emitted, correctEmissions, p50, p95);
    }

    private static GeometricRanker.Point point(String x, String y) {
        return new GeometricRanker.Point(Double.parseDouble(x), Double.parseDouble(y));
    }
}
