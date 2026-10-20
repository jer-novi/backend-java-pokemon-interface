package audio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-Kanaals Chiptune JSON Synthesizer met Jackson & Live Tempo-schaling!
 *
 * Leest met Jackson's ObjectMapper in één regel code een meer-kanaals JSON-track in
 * (Poke-red-og.json, poke-2nd-choice.json of poke-3rd-choice.json) naar simpele
 * Java records en mixt alle kanalen op het gekozen tempo (0.5x t/m 2.0x) zonder
 * dat de toonhoogte (pitch) vervormt!
 */
public final class ChiptuneJsonSynth {

    public static final int SAMPLE_RATE = 22050;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<Path, ChiptuneSong> SONG_CACHE = new HashMap<>();
    private static final Map<String, byte[]> PCM_CACHE = new HashMap<>();

    private ChiptuneJsonSynth() {
    }

    // =========================================================================
    // 1. JACKSON DATA-MODEL (5 SIMPELE JAVA RECORDS!)
    // =========================================================================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChiptuneSong(
            String name,
            int beatsPerMinute,
            int beatsPerBar,
            int ticksPerBeat,
            List<Channel> channels
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Channel(
            String type,
            List<PatternData> patterns,
            List<Integer> sequence
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PatternData(
            List<Note> notes
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Note(
            List<Integer> pitches,
            List<Point> points
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Point(
            int tick,
            int volume
    ) {}

    // =========================================================================
    // 2. AUDIO SYNTHESIZER (MET LIVE TEMPO MULTIPLIER ZONDER PITCH-VERVORMING!)
    // =========================================================================

    public static synchronized byte[] getPcmBuffer(Path jsonPath, double tempoMultiplier) throws Exception {
        int tempoKey = (int) Math.round(tempoMultiplier * 100);
        String cacheKey = jsonPath.toString() + "@" + tempoKey;

        byte[] pcmBuffer = PCM_CACHE.get(cacheKey);
        if (pcmBuffer == null) {
            pcmBuffer = synthesizeSong(jsonPath, tempoMultiplier);
            PCM_CACHE.put(cacheKey, pcmBuffer);
        }
        return pcmBuffer;
    }

    public static byte[] synthesizeSong(Path jsonPath, double tempoMultiplier) throws Exception {
        ChiptuneSong song = SONG_CACHE.get(jsonPath);
        if (song == null) {
            song = MAPPER.readValue(jsonPath.toFile(), ChiptuneSong.class);
            SONG_CACHE.put(jsonPath, song);
        }

        if (song.channels() == null || song.channels().isEmpty()) {
            throw new IllegalArgumentException("Geen kanalen gevonden in " + jsonPath);
        }

        double clampedTempo = Math.max(0.50, Math.min(2.00, tempoMultiplier));
        int baseBpm = song.beatsPerMinute() > 0 ? song.beatsPerMinute() : 190;
        double effectiveBpm = baseBpm * clampedTempo;
        int beatsPerBar = song.beatsPerBar() > 0 ? song.beatsPerBar() : 8;
        int ticksPerBeat = song.ticksPerBeat() > 0 ? song.ticksPerBeat() : 4;
        int ticksPerBar = beatsPerBar * ticksPerBeat;
        double tickSec = (60.0 / effectiveBpm) / ticksPerBeat;

        int numBars = song.channels().get(0).sequence().size();
        int totalTicks = numBars * ticksPerBar;
        int totalSamples = Math.max(1, (int) (totalTicks * tickSec * SAMPLE_RATE));

        float[] mix = new float[totalSamples];

        for (int chIdx = 0; chIdx < song.channels().size(); chIdx++) {
            Channel ch = song.channels().get(chIdx);
            if (ch.sequence() == null || ch.patterns() == null) continue;

            boolean isPitch = "pitch".equalsIgnoreCase(ch.type());
            boolean isDrum = "drum".equalsIgnoreCase(ch.type());
            if (!isPitch && !isDrum) {
                continue;
            }

            float chWeight = (chIdx == 0) ? 0.28f : (isDrum ? 0.12f : 0.18f);
            double dutyCycle = (chIdx == 0) ? 0.25 : 0.50;

            for (int barIdx = 0; barIdx < ch.sequence().size(); barIdx++) {
                int patId = ch.sequence().get(barIdx);
                if (patId <= 0 || patId > ch.patterns().size()) {
                    continue;
                }
                List<Note> notes = ch.patterns().get(patId - 1).notes();
                if (notes == null) continue;
                int barStartTick = barIdx * ticksPerBar;

                for (Note note : notes) {
                    if (note.pitches() == null || note.pitches().isEmpty()
                            || note.points() == null || note.points().size() < 2) {
                        continue;
                    }
                    Point firstPt = note.points().get(0);
                    Point lastPt = note.points().get(note.points().size() - 1);
                    int t0 = barStartTick + firstPt.tick();
                    int t1 = barStartTick + lastPt.tick();
                    float v0 = firstPt.volume() / 100.0f;
                    float v1 = lastPt.volume() / 100.0f;

                    int s0 = (int) (t0 * tickSec * SAMPLE_RATE);
                    int s1 = Math.min(totalSamples, (int) (t1 * tickSec * SAMPLE_RATE));
                    if (s1 <= s0) continue;

                    if (isPitch) {
                        double[] freqs = new double[note.pitches().size()];
                        for (int i = 0; i < freqs.length; i++) {
                            freqs[i] = 440.0 * Math.pow(2.0, (note.pitches().get(i) - 69.0) / 12.0);
                        }
                        for (int s = s0; s < s1; s++) {
                            float prog = (float) (s - s0) / (s1 - s0);
                            float vol = (v0 + (v1 - v0) * prog) * chWeight;
                            double f = freqs[((int) ((s / (double) SAMPLE_RATE) * 30)) % freqs.length];
                            double phase = (s * f / SAMPLE_RATE) % 1.0;
                            mix[s] += (phase < dutyCycle) ? vol : -vol;
                        }
                    } else {
                        int lfsr = 0x7FFF;
                        int drumEnd = Math.min(s1, s0 + (int) (0.08 * SAMPLE_RATE));
                        for (int s = s0; s < drumEnd; s++) {
                            float prog = (float) (s - s0) / Math.max(1, (drumEnd - s0));
                            float vol = (1.0f - prog) * chWeight * v0;
                            if (s % 4 == 0) {
                                int bit = ((lfsr) ^ (lfsr >> 1)) & 1;
                                lfsr = (lfsr >> 1) | (bit << 14);
                            }
                            mix[s] += ((lfsr & 1) != 0) ? vol : -vol;
                        }
                    }
                }
            }
        }

        float maxPeak = 0.01f;
        for (float sample : mix) {
            float abs = Math.abs(sample);
            if (abs > maxPeak) maxPeak = abs;
        }
        float normFactor = 0.80f / maxPeak;

        byte[] pcm = new byte[totalSamples * 2];
        for (int i = 0; i < totalSamples; i++) {
            float normalized = Math.max(-0.95f, Math.min(0.95f, mix[i] * normFactor));
            short sample = (short) (normalized * 32767);
            pcm[i * 2] = (byte) (sample & 0xFF);
            pcm[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return pcm;
    }

    public static void main(String[] args) throws Exception {
        for (SoundEngine.MusicMode mode : List.of(
                SoundEngine.MusicMode.MULTI_1,
                SoundEngine.MusicMode.MULTI_2,
                SoundEngine.MusicMode.MULTI_3)) {
            long t0 = System.currentTimeMillis();
            byte[] pcm = synthesizeSong(mode.getJsonPath(), 1.0);
            long ms = System.currentTimeMillis() - t0;
            double sec = (pcm.length / 2.0) / SAMPLE_RATE;
            System.out.printf("✅ %s (%s) -> Jackson parsed & mixed in %d ms (Duur: %.1f sec)%n",
                    mode.getLabel(), mode.getJsonPath(), ms, sec);
        }
    }
}
