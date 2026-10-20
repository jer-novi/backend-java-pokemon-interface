package audio;

import Pokemon.PokemonType;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 8-Bit GameBoy Chiptune Sound Engine!
 *
 * Streamt alle muziek (Multi-Channel JSON én 1-Channel Array) in blokjes van 40ms
 * over één geopend SourceDataLine-kanaal in een Daemon Thread:
 * - Geen 'Java Sound Event Dispatcher' thread die 15 seconden blijft hangen bij het afsluiten!
 * - Directe Volume-aanpassing (0% - 100%) en Tempo-aanpassing (0.50x - 2.00x) binnen 40ms
 *   zonder hapering en zonder het nummer opnieuw vanaf het begin te starten!
 */
public final class SoundEngine {

    public enum MusicMode {
        MULTI_1("Multi-Ch #1 (Poke-Red-OG)", Path.of("assets", "music", "Poke-red-og.json")),
        MULTI_2("Multi-Ch #2 (Poke-2nd-Choice)", Path.of("assets", "music", "poke-2nd-choice.json")),
        MULTI_3("Multi-Ch #3 (Poke-3rd-Choice)", Path.of("assets", "music", "poke-3rd-choice.json")),
        SINGLE("1-Channel Classic Pokémon Array", null),
        SFX_ONLY("SFX Only (No Background Music)", null);

        private final String label;
        private final Path jsonPath;

        MusicMode(String label, Path jsonPath) {
            this.label = label;
            this.jsonPath = jsonPath;
        }

        public String getLabel() {
            return label;
        }

        public Path getJsonPath() {
            return jsonPath;
        }
    }

    public static final double MIN_TEMPO = 0.50;
    public static final double MAX_TEMPO = 2.00;
    public static final int MIN_VOLUME = 10;
    public static final int MAX_VOLUME = 100;
    public static final int SLIDER_WIDTH = 17;

    private static final float SFX_SAMPLE_RATE = 16000f;
    private static final int BGM_SAMPLE_RATE = ChiptuneJsonSynth.SAMPLE_RATE; // 22050 Hz, 16-bit
    private static final AtomicBoolean muted = new AtomicBoolean(false);
    private static final AtomicBoolean bgmRunning = new AtomicBoolean(false);

    private static volatile MusicMode currentMode = MusicMode.MULTI_1;
    private static volatile double tempoMultiplier = 1.00;
    private static volatile int volumePercent = 70; // Standaard 70% volume
    private static volatile byte[] activeBgmPcm;
    private static volatile double pendingSeekRatio = -1.0;

    private static Thread bgmThread;
    private static final Map<Integer, byte[]> SINGLE_CHANNEL_CACHE = new HashMap<>();

    // De 1-kanaals GameBoy Trainer Battle melodie als {Frequentie in Hz, Duur in ms} paren!
    private static final int[][] POKEMON_TRAINER_SINGLE_CHANNEL = {
            // Iconische chromatische Pokémon Trainer Battle intro-cascade!
            {659, 140}, {622, 70}, {587, 70}, {622, 140}, {587, 70}, {554, 70},
            {587, 140}, {554, 70}, {523, 70}, {554, 140}, {523, 70}, {494, 70},
            {523, 140}, {494, 70}, {466, 70}, {494, 140}, {466, 70}, {440, 70},
            {466, 140}, {440, 70}, {415, 70}, {440, 140}, {415, 70}, {392, 70},
            // Spannende staccato bas-puls + riff
            {262, 80}, {0, 240}, {262, 80}, {0, 240}, {262, 80}, {0, 160},
            {262, 80}, {0, 320}, {262, 140}, {277, 140}, {233, 140}, {277, 140},
            {262, 80}, {0, 240}, {262, 80}, {0, 240}, {262, 80}, {0, 160},
            {262, 80}, {0, 320}, {262, 140}, {277, 140}, {330, 140}, {349, 140},
            {392, 80}, {0, 240}, {392, 80}, {0, 240}, {392, 80}, {0, 160},
            {392, 80}, {0, 320}, {392, 140}, {415, 140}, {349, 140}, {415, 140},
            {392, 80}, {0, 240}, {392, 80}, {0, 240}, {392, 80}, {0, 160},
            {392, 80}, {0, 320}, {392, 140}, {415, 140}, {466, 140}, {494, 140},
            // Hoofdthema Trainer Battle melodie
            {523, 380}, {392, 380}, {523, 260}, {466, 260}, {415, 260}, {392, 260},
            {349, 260}, {330, 90}, {0, 90}, {349, 90}, {392, 260}, {0, 220},
            {587, 90}, {0, 90}, {349, 90}, {466, 900}, {523, 380}, {392, 380},
            {523, 260}, {466, 260}, {415, 260}, {392, 260}, {349, 260}, {330, 90},
            {0, 90}, {349, 90}, {392, 260}, {0, 220}, {523, 90}, {0, 90},
            {587, 90}, {659, 500}, {587, 500}
    };

    private SoundEngine() {
    }

    public static void setMusicMode(MusicMode mode) {
        currentMode = mode;
        if (bgmRunning.get()) {
            reloadActiveBuffer(true);
        }
    }

    public static MusicMode getMusicMode() {
        return currentMode;
    }

    public static double getTempoMultiplier() {
        return tempoMultiplier;
    }

    public static int getVolumePercent() {
        return volumePercent;
    }

    public static synchronized int adjustVolume(int deltaPercent) {
        return setVolumePercent(volumePercent + deltaPercent);
    }

    public static synchronized int setVolumePercent(int newVolume) {
        volumePercent = Math.max(10, Math.min(100, (newVolume / 5) * 5));
        if (!bgmRunning.get() && !muted.get()) {
            playSelectBeep();
        }
        return volumePercent;
    }

    public static synchronized double adjustTempo(double delta) {
        return setTempoMultiplier(tempoMultiplier + delta);
    }

    public static synchronized double setTempoMultiplier(double newTempo) {
        double clamped = Math.max(MIN_TEMPO, Math.min(MAX_TEMPO, newTempo));
        tempoMultiplier = Math.round(clamped * 20.0) / 20.0;
        if (bgmRunning.get()) {
            reloadActiveBuffer(false);
        } else if (!muted.get()) {
            playSelectBeep();
        }
        return tempoMultiplier;
    }

    /** Tekent de visuele ASCII Volume Slider: "[-] [███████████░░░░░░] [+]  70%" */
    public static String renderVolumeSlider() {
        int filled = (int) Math.round((volumePercent / 100.0) * SLIDER_WIDTH);
        filled = Math.max(0, Math.min(SLIDER_WIDTH, filled));
        String bar = "█".repeat(filled) + "░".repeat(SLIDER_WIDTH - filled);
        return String.format("[-] [%s] [+] %3d%%", bar, volumePercent);
    }

    /** Tekent de visuele ASCII Tempo Slider: "[-] [────────●────────] [+] 1.00x (100%)" */
    public static String renderTempoSlider() {
        double ratio = (tempoMultiplier - MIN_TEMPO) / (MAX_TEMPO - MIN_TEMPO);
        int knobPos = (int) Math.round(ratio * (SLIDER_WIDTH - 1));
        knobPos = Math.max(0, Math.min(SLIDER_WIDTH - 1, knobPos));

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < SLIDER_WIDTH; i++) {
            bar.append(i == knobPos ? "●" : "─");
        }
        int pct = (int) Math.round(tempoMultiplier * 100);
        return String.format(Locale.US, "[-] [%s] [+] %4.2fx (%3d%%)", bar, tempoMultiplier, pct);
    }

    public static synchronized MusicMode cycleNextTrack() {
        return cycleTrack(+1);
    }

    public static synchronized MusicMode cycleTrack(int delta) {
        MusicMode[] modes = MusicMode.values();
        int nextIndex = Math.floorMod(currentMode.ordinal() + delta, modes.length);
        currentMode = modes[nextIndex];
        muted.set(false);
        if (currentMode == MusicMode.SFX_ONLY) {
            stopBattleMusic();
            playSelectBeep();
        } else if (bgmRunning.get()) {
            reloadActiveBuffer(true);
        } else {
            startBattleMusic();
        }
        return currentMode;
    }

    public static String getStatusText() {
        int trackNum = currentMode.ordinal() + 1;
        int totalTracks = MusicMode.values().length;
        if (muted.get()) {
            return "🔇 MUTED  ['s'=Settings | 'm'=Unmute | 't'=Next Track | 'q'=Quit]";
        }
        return String.format(Locale.US, "🔊 [%d/%d] %s (%.2fx, Vol %d%%)",
                trackNum, totalTracks, currentMode.getLabel(), tempoMultiplier, volumePercent);
    }

    public static boolean toggleMute() {
        boolean nowMuted = !muted.get();
        muted.set(nowMuted);
        if (nowMuted) {
            stopBattleMusic();
        } else {
            playSelectBeep();
            startBattleMusic();
        }
        return !nowMuted;
    }

    public static boolean isMuted() {
        return muted.get();
    }

    public static void setMuted(boolean mute) {
        muted.set(mute);
        if (mute) {
            stopBattleMusic();
        }
    }

    // =========================================================================
    // 1. ACHTERGRONDMUZIEK STREAMER (40MS CHUNKS IN EEN DAEMON THREAD)
    // =========================================================================

    private static synchronized void reloadActiveBuffer(boolean resetPosition) {
        try {
            if (currentMode == MusicMode.SFX_ONLY) {
                activeBgmPcm = null;
                return;
            }
            if (currentMode.getJsonPath() != null && Files.exists(currentMode.getJsonPath())) {
                activeBgmPcm = ChiptuneJsonSynth.getPcmBuffer(currentMode.getJsonPath(), tempoMultiplier);
            } else {
                activeBgmPcm = getSingleChannelPcm(tempoMultiplier);
            }
            if (resetPosition) {
                pendingSeekRatio = 0.0;
            }
        } catch (Exception ignored) {
        }
    }

    public static synchronized void startBattleMusic() {
        if (muted.get() || currentMode == MusicMode.SFX_ONLY) {
            return;
        }
        reloadActiveBuffer(true);
        if (bgmRunning.get() || activeBgmPcm == null) {
            return;
        }
        bgmRunning.set(true);

        bgmThread = new Thread(() -> {
            AudioFormat format = new AudioFormat(BGM_SAMPLE_RATE, 16, 1, true, false);
            int chunkSamples = BGM_SAMPLE_RATE / 25; // 40ms blokjes = 882 samples (1764 bytes)
            int chunkBytes = chunkSamples * 2;
            byte[] scaledChunk = new byte[chunkBytes];

            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format, chunkBytes * 4);
                line.start();

                int bytePos = 0;
                byte[] currentBuf = activeBgmPcm;

                while (bgmRunning.get() && !muted.get() && currentMode != MusicMode.SFX_ONLY) {
                    byte[] latestBuf = activeBgmPcm;
                    if (latestBuf == null || latestBuf.length < 2) break;

                    if (latestBuf != currentBuf) {
                        // Als het tempo is veranderd, spring naar dezelfde relatieve positie in het nummer!
                        double ratio = (currentBuf != null && currentBuf.length > 0)
                                ? (bytePos / (double) currentBuf.length) : 0.0;
                        currentBuf = latestBuf;
                        bytePos = ((int) (ratio * (currentBuf.length / 2))) * 2;
                    }
                    if (pendingSeekRatio >= 0.0) {
                        bytePos = ((int) (pendingSeekRatio * (currentBuf.length / 2))) * 2;
                        pendingSeekRatio = -1.0;
                    }
                    if (bytePos >= currentBuf.length) {
                        bytePos = 0; // Loop naadloos naar het begin!
                    }

                    int bytesToWrite = Math.min(chunkBytes, currentBuf.length - bytePos);
                    // Schaal het volume live per 40ms blokje!
                    float volScale = (volumePercent / 100.0f) * 0.45f;
                    for (int i = 0; i < bytesToWrite; i += 2) {
                        short raw = (short) ((currentBuf[bytePos + i] & 0xFF) | (currentBuf[bytePos + i + 1] << 8));
                        short scaled = (short) Math.max(-32767, Math.min(32767, raw * volScale));
                        scaledChunk[i] = (byte) (scaled & 0xFF);
                        scaledChunk[i + 1] = (byte) ((scaled >> 8) & 0xFF);
                    }
                    line.write(scaledChunk, 0, bytesToWrite);
                    bytePos += bytesToWrite;
                }
                line.flush();
            } catch (Exception ignored) {
            } finally {
                bgmRunning.set(false);
            }
        }, "Pokemon-BGM-Streamer");
        bgmThread.setDaemon(true); // Daemon thread sluit in 0ms af zodra Main stopt!
        bgmThread.start();
    }

    /**
     * Zet de 1-kanaals {Hz, ms} array (POKEMON_TRAINER_SINGLE_CHANNEL) om naar
     * 16-bit Little-Endian PCM op 22050 Hz zodat dezelfde streamer hem afspeelt.
     */
    private static synchronized byte[] getSingleChannelPcm(double tempo) {
        int tempoKey = (int) Math.round(tempo * 100);
        byte[] pcm = SINGLE_CHANNEL_CACHE.get(tempoKey);
        if (pcm != null) {
            return pcm;
        }

        int gapMs = Math.max(5, (int) Math.round(12 / tempo));
        int totalMs = 0;
        for (int[] note : POKEMON_TRAINER_SINGLE_CHANNEL) {
            int scaledDuration = Math.max(15, (int) Math.round(note[1] / tempo));
            totalMs += scaledDuration + (note[0] > 0 ? gapMs : 0);
        }
        int totalSamples = (int) ((long) BGM_SAMPLE_RATE * totalMs / 1000);
        byte[] buf = new byte[totalSamples * 2];
        int sampleCursor = 0;
        short amp = 14000;

        for (int[] note : POKEMON_TRAINER_SINGLE_CHANNEL) {
            int hz = note[0];
            int scaledMs = Math.max(15, (int) Math.round(note[1] / tempo));
            int noteSamples = (int) ((long) BGM_SAMPLE_RATE * scaledMs / 1000);
            if (hz > 0) {
                int period = Math.max(2, BGM_SAMPLE_RATE / hz);
                for (int i = 0; i < noteSamples && sampleCursor < totalSamples; i++) {
                    short val = (i % period < period / 2) ? amp : (short) -amp;
                    buf[sampleCursor * 2] = (byte) (val & 0xFF);
                    buf[sampleCursor * 2 + 1] = (byte) ((val >> 8) & 0xFF);
                    sampleCursor++;
                }
                int gapSamples = (int) ((long) BGM_SAMPLE_RATE * gapMs / 1000);
                sampleCursor = Math.min(totalSamples, sampleCursor + gapSamples);
            } else {
                sampleCursor = Math.min(totalSamples, sampleCursor + noteSamples);
            }
        }
        SINGLE_CHANNEL_CACHE.put(tempoKey, buf);
        return buf;
    }

    public static synchronized void stopBattleMusic() {
        bgmRunning.set(false);
        if (bgmThread != null) {
            bgmThread.interrupt();
            bgmThread = null;
        }
    }

    // =========================================================================
    // 2. SPECIFIEKE GAMEBOY SOUND EFFECTS (SFX)
    // =========================================================================

    public static void playSelectBeep() {
        if (muted.get()) return;
        runAsync(() -> playSquareWave(1046, 45, 30));
    }

    public static void playPokeballWobble() {
        if (muted.get()) return;
        runAsync(() -> {
            playFrequencySlide(220, 380, 65, 28);
            sleepQuietly(30);
            playFrequencySlide(380, 190, 75, 28);
        });
    }

    public static void playPokeballOpen() {
        if (muted.get()) return;
        runAsync(() -> {
            playFrequencySlide(300, 1200, 110, 32);
            playSquareWave(1568, 70, 32);
        });
    }

    public static void playHealJingle() {
        if (muted.get()) return;
        runAsync(() -> {
            playSquareWave(987, 130, 35);
            sleepQuietly(40);
            playSquareWave(987, 130, 35);
            sleepQuietly(40);
            playSquareWave(987, 130, 35);
            sleepQuietly(40);
            playSquareWave(830, 130, 35);
            sleepQuietly(40);
            playSquareWave(1318, 320, 38);
        });
    }

    public static void playAttackSfx(PokemonType type) {
        if (muted.get()) return;
        runAsync(() -> {
            if (type == null) {
                playSquareWave(440, 100, 35);
                return;
            }
            switch (type) {
                case FIRE -> {
                    playFrequencySlide(450, 140, 160, 40);
                    playFrequencySlide(350, 90, 140, 40);
                }
                case ELECTRIC -> {
                    playSquareWave(1760, 40, 38);
                    playSquareWave(880, 40, 38);
                    playSquareWave(1975, 45, 38);
                    playSquareWave(987, 45, 38);
                    playSquareWave(2093, 80, 40);
                }
                case WATER -> {
                    playFrequencySlide(300, 900, 90, 35);
                    playFrequencySlide(900, 400, 90, 35);
                    playFrequencySlide(400, 1000, 90, 35);
                }
                case GRASS -> {
                    playFrequencySlide(1200, 600, 55, 35);
                    sleepQuietly(25);
                    playFrequencySlide(1400, 700, 55, 35);
                    sleepQuietly(25);
                    playFrequencySlide(1600, 800, 70, 35);
                }
            }
        });
    }

    public static void playSuperHitSfx() {
        if (muted.get()) return;
        runAsync(() -> {
            playSquareWave(220, 60, 45);
            playSquareWave(165, 60, 45);
            playSquareWave(110, 120, 45);
        });
    }

    public static void playLowHpWarning() {
        if (muted.get()) return;
        runAsync(() -> {
            for (int i = 0; i < 2; i++) {
                playSquareWave(987, 90, 35);
                playSquareWave(784, 110, 35);
                sleepQuietly(40);
            }
        });
    }

    public static void playFaintSfx() {
        if (muted.get()) return;
        runAsync(() -> {
            playFrequencySlide(600, 350, 140, 38);
            playFrequencySlide(350, 200, 140, 38);
            playFrequencySlide(200, 90, 220, 38);
        });
    }

    public static void playVictoryFanfare() {
        if (muted.get()) return;
        stopBattleMusic();
        runAsync(() -> {
            playSquareWave(523, 90, 38);
            sleepQuietly(25);
            playSquareWave(523, 90, 38);
            sleepQuietly(25);
            playSquareWave(523, 90, 38);
            sleepQuietly(25);
            playSquareWave(523, 240, 38);
            sleepQuietly(40);
            playSquareWave(415, 220, 38);
            playSquareWave(466, 220, 38);
            playSquareWave(523, 160, 38);
            sleepQuietly(30);
            playSquareWave(466, 90, 38);
            playSquareWave(523, 450, 42);
        });
    }

    private static void runAsync(Runnable r) {
        Thread t = new Thread(r, "GameBoy-SFX");
        t.setDaemon(true);
        t.start();
    }

    private static void sleepQuietly(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void playSquareWave(int hz, int durationMs, int volume) {
        if (muted.get() || hz <= 0) return;
        try {
            byte[] buf = new byte[(int) (SFX_SAMPLE_RATE * durationMs / 1000)];
            int period = Math.max(2, (int) (SFX_SAMPLE_RATE / hz));
            float scale = volumePercent / 100.0f;
            byte amp = (byte) Math.min(120, Math.max(1, Math.round(volume * scale)));
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (i % period < period / 2) ? amp : (byte) -amp;
            }
            AudioFormat af = new AudioFormat(SFX_SAMPLE_RATE, 8, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
                line.open(af, buf.length);
                line.start();
                line.write(buf, 0, buf.length);
                line.drain();
            }
        } catch (Exception ignored) {
        }
    }

    private static void playFrequencySlide(int startHz, int endHz, int durationMs, int volume) {
        if (muted.get()) return;
        try {
            int totalSamples = (int) (SFX_SAMPLE_RATE * durationMs / 1000);
            byte[] buf = new byte[totalSamples];
            float scale = volumePercent / 100.0f;
            byte amp = (byte) Math.min(120, Math.max(1, Math.round(volume * scale)));
            double phase = 0.0;
            for (int i = 0; i < totalSamples; i++) {
                double progress = (double) i / totalSamples;
                double currentHz = startHz + (endHz - startHz) * progress;
                phase += currentHz / SFX_SAMPLE_RATE;
                buf[i] = ((phase % 1.0) < 0.5) ? amp : (byte) -amp;
            }
            AudioFormat af = new AudioFormat(SFX_SAMPLE_RATE, 8, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
                line.open(af, buf.length);
                line.start();
                line.write(buf, 0, buf.length);
                line.drain();
            }
        } catch (Exception ignored) {
        }
    }
}
