package ui;

import Pokemon.Pokemon;
import Pokemon.PokemonType;
import audio.SoundEngine;
import io.GameIO;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ArcadeUI bevat alle visuele en retro terminal-effecten voor de Pokémon Gym:
 * 1. Visuele gekleurde HP-balken
 * 2. ASCII-art lader uit assets/ASCII/ (met keuze tussen Normal en Colored Pixel-art + automatische rechter-uitlijning!)
 * 3. Typewriter-effect voor RPG-dialogen (met instelbare snelheid!)
 * 4. Gekleurde type-labels, strakke 6v6 gevechts-HUD en In-Place Settings Screen ('s')
 * 5. Instelbare Regelafstand (Compact / Normaal / Ruim)
 * 6. Save-game & Highscore systeem (pokemon_savegame.txt)
 * 7. Divoom Pixoo Max / Home Assistant Live State Sync (divoom_state.json)
 */
public final class ArcadeUI {

    public enum LineSpacing {
        COMPACT("Compact (0 witregels — voorkomt scrollen)", 0),
        NORMAL("Normaal (1 witregel tussen secties)", 1),
        SPACIOUS("Ruim (2 witregels tussen secties)", 2);

        private final String label;
        private final int blankLines;

        LineSpacing(String label, int blankLines) {
            this.label = label;
            this.blankLines = blankLines;
        }

        public String getLabel() {
            return label;
        }

        public int getBlankLines() {
            return blankLines;
        }
    }

    public enum TextSpeed {
        INSTANT("Instant (0 ms — direct op scherm)", 0),
        FAST("Snel (5 ms per letter)", 5),
        NORMAL("Normaal (12 ms per letter)", 12),
        RETRO("Retro RPG (25 ms per letter)", 25);

        private final String label;
        private final int delayMs;

        TextSpeed(String label, int delayMs) {
            this.label = label;
            this.delayMs = delayMs;
        }

        public String getLabel() {
            return label;
        }

        public int getDelayMs() {
            return delayMs;
        }
    }

    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";

    private static final Path SAVE_FILE = Path.of("pokemon_savegame.txt");
    private static final Path DIVOOM_STATE_FILE = Path.of("divoom_state.json");
    private static final Path ASCII_DIR = Path.of("assets", "ASCII");

    private static boolean useColoredArt = true;
    private static LineSpacing lineSpacing = LineSpacing.NORMAL;
    private static TextSpeed textSpeed = TextSpeed.NORMAL;

    private ArcadeUI() {
    }

    public static void setUseColoredArt(boolean colored) {
        useColoredArt = colored;
    }

    public static boolean isUseColoredArt() {
        return useColoredArt;
    }

    public static LineSpacing getLineSpacing() {
        return lineSpacing;
    }

    public static void cycleLineSpacing() {
        cycleLineSpacing(+1);
    }

    public static void cycleLineSpacing(int delta) {
        LineSpacing[] vals = LineSpacing.values();
        lineSpacing = vals[Math.floorMod(lineSpacing.ordinal() + delta, vals.length)];
    }

    public static TextSpeed getTextSpeed() {
        return textSpeed;
    }

    public static void cycleTextSpeed() {
        cycleTextSpeed(+1);
    }

    public static void cycleTextSpeed(int delta) {
        TextSpeed[] vals = TextSpeed.values();
        textSpeed = vals[Math.floorMod(textSpeed.ordinal() + delta, vals.length)];
    }

    /** Print het ingestelde aantal lege regels (0 bij Compact, 1 bij Normaal, 2 bij Ruim). */
    public static void printSectionSpacing(GameIO io) {
        for (int i = 0; i < lineSpacing.getBlankLines(); i++) {
            io.println("");
        }
    }

    /** 1. Visuele HP-balk die van kleur verandert (Groen -> Geel -> Rood). */
    public static String renderHpBar(Pokemon p) {
        int hp = Math.max(0, p.getHp());
        int maxHp = Math.max(1, p.getMaxHp());
        int barLength = 20;
        int filled = (int) Math.round(((double) hp / maxHp) * barLength);
        filled = Math.min(barLength, Math.max(0, filled));
        int empty = barLength - filled;

        double ratio = (double) hp / maxHp;
        String color;
        if (ratio > 0.5) {
            color = GREEN;
        } else if (ratio > 0.2) {
            color = YELLOW;
        } else {
            color = RED;
        }

        String bar = "█".repeat(filled) + "░".repeat(empty);
        String paddedName = String.format("%-11s", p.getName());
        String typeBadge = formatTypeBadge(p.getPokemonType());
        return String.format("%s %s %s[%s]%s %3d/%3d HP",
                BOLD + paddedName + RESET,
                typeBadge,
                color, bar, RESET,
                hp, maxHp);
    }

    /** 4. Gekleurde Type-badge met vaste breedte voor perfecte kolom-uitlijning. */
    public static String formatTypeBadge(PokemonType type) {
        if (type == null) return "[UNKNOWN ]";
        return switch (type) {
            case FIRE -> RED + "[FIRE    ]" + RESET;
            case WATER -> BLUE + "[WATER   ]" + RESET;
            case GRASS -> GREEN + "[GRASS   ]" + RESET;
            case ELECTRIC -> YELLOW + "[ELECTRIC]" + RESET;
        };
    }

    /**
     * Strakke, rustige Battle HUD (zonder losse sliders die het gevecht verstoren).
     * Alle sliders en visuele instellingen zitten netjes onder de knop ['s' = Settings]!
     */
    public static void printBattleHud(GameIO io, String playerName, Pokemon playerPokemon, List<Pokemon> playerParty,
                                      String ownerName, Pokemon gymPokemon, List<Pokemon> gymParty) {
        String ownerLabel = String.format("%-9s", ownerName + ":");
        String playerLabel = String.format("%-9s", playerName + ":");

        int playerAlive = (int) playerParty.stream().filter(p -> !p.isFainted()).count();
        int gymAlive = (int) gymParty.stream().filter(p -> !p.isFainted()).count();
        String headerText = String.format("PEWTER CITY GYM (6v6) — %s [%d/6] vs %s [%d/6]",
                ownerName, gymAlive, playerName, playerAlive);
        String paddedHeader = String.format("%-67s", headerText);

        int trackIdx = SoundEngine.getMusicMode().ordinal() + 1;
        int totalTracks = SoundEngine.MusicMode.values().length;
        String soundState = SoundEngine.isMuted()
                ? "Audio: MUTED   ['s'=Settings | 'm'=Unmute | 't'=Next Track | 'q'=Quit]"
                : String.format(Locale.US, "Track [%d/%d] %.2fx  ['s'=Settings | 'm'=Mute | 't'=Next Track | 'q'=Quit]",
                trackIdx, totalTracks, SoundEngine.getTempoMultiplier());
        String shortcutsRow = String.format("%-67s", soundState);

        io.println(CYAN + "╔═════════════════════════════════════════════════════════════════════╗" + RESET);
        io.println(CYAN + "║ " + RESET + BOLD + paddedHeader + RESET + CYAN + " ║" + RESET);
        io.println(CYAN + "╠═════════════════════════════════════════════════════════════════════╣" + RESET);
        io.println(CYAN + "║ " + RESET + RED + ownerLabel + RESET + " " + renderHpBar(gymPokemon) + CYAN + " ║" + RESET);
        io.println(CYAN + "║ " + RESET + GREEN + playerLabel + RESET + " " + renderHpBar(playerPokemon) + CYAN + " ║" + RESET);
        io.println(CYAN + "╠═════════════════════════════════════════════════════════════════════╣" + RESET);
        io.println(CYAN + "║ " + RESET + YELLOW + shortcutsRow + RESET + CYAN + " ║" + RESET);
        io.println(CYAN + "╚═════════════════════════════════════════════════════════════════════╝" + RESET);
    }

    /** Herstelt alle audio- en layout-instellingen naar de standaardwaarden. */
    public static void resetDefaults() {
        useColoredArt = true;
        lineSpacing = LineSpacing.NORMAL;
        textSpeed = TextSpeed.NORMAL;
        SoundEngine.setVolumePercent(70);
        SoundEngine.setTempoMultiplier(1.00);
    }

    /** Exacte hoogte (in regels) van het Settings Pop-up Venster voor naadloos in-place hertekenen & sluiten. */
    public static final int SETTINGS_BOX_HEIGHT = 18;

    public static void renderSettingsDashboard(GameIO io) {
        renderSettingsDashboard(io, 3, true);
    }

    /**
     * In-Place Pop-up Settings Venster (te openen met 's' op elk moment in het spel!).
     * Tekent zichzelf op de Alternate Screen Buffer vanaf rij 0, kolom 0 zodat alle muis-coördinaten (x, y)
     * voor klikken op 1)..8) en op de Volume/Tempo sliders altijd 100% exact kloppen, en bij het sluiten
     * 100% van je voorgaande gevechts-log bewaard blijft!
     */
    public static void renderSettingsDashboard(GameIO io, int selectedRow, boolean isFirstDraw) {
        if (!isFirstDraw) {
            // Zet de cursor terug naar linksboven (rij 0, kolom 0) van het Settings-venster zonder te flikkeren
            System.out.print("\033[H");
            System.out.flush();
        }

        String soundStatus = SoundEngine.isMuted()
                ? RED + "MUTED (Uit)  [Klik of ←/→ om AAN te zetten]" + RESET
                : GREEN + "ON (Actief)  [Klik of ←/→ om te Muten]" + RESET;
        int trackIdx = SoundEngine.getMusicMode().ordinal() + 1;
        int totalTracks = SoundEngine.MusicMode.values().length;
        String trackLabel = "[" + trackIdx + "/" + totalTracks + "] " + SoundEngine.getMusicMode().getLabel();
        if (trackLabel.length() > 41) {
            trackLabel = trackLabel.substring(0, 41);
        }
        String artLabel = useColoredArt ? GREEN + "Color Pixel-Art (Divoom)" + RESET : CYAN + "Classic ASCII" + RESET;
        String resetLabel = CYAN + "[Klik op 8) of druk 8 om te resetten]" + RESET;

        io.println(CYAN + "╔═════════════════════════════════════════════════════════════════════╗" + RESET);
        io.println(CYAN + "║ " + RESET + YELLOW + BOLD + "⚙️  POKÉMON GYM — SETTINGS MENU                      " + RED + BOLD + "[✖ SLUITEN: 0]" + RESET + CYAN + " ║" + RESET);
        io.println(CYAN + "╠═════════════════════════════════════════════════════════════════════╣" + RESET);
        io.println(formatSettingRow(1, selectedRow, "🔊 Geluid Aan/Uit  ", soundStatus));
        io.println(formatSettingRow(2, selectedRow, "🎵 Soundtrack (1-5)", trackLabel));
        io.println(formatSettingRow(3, selectedRow, "🎚️ Muziek Volume   ", GREEN + SoundEngine.renderVolumeSlider() + RESET));
        io.println(formatSettingRow(4, selectedRow, "⏱️ Muziek Tempo    ", YELLOW + SoundEngine.renderTempoSlider() + RESET));
        io.println(formatSettingRow(5, selectedRow, "🎨 ASCII Art Stijl ", artLabel));
        io.println(formatSettingRow(6, selectedRow, "📏 Regelafstand    ", lineSpacing.getLabel()));
        io.println(formatSettingRow(7, selectedRow, "⌨️ Tekstsnelheid   ", textSpeed.getLabel()));
        io.println(formatSettingRow(8, selectedRow, "🔄 Reset Standaard ", resetLabel));
        io.println(CYAN + "╠═════════════════════════════════════════════════════════════════════╣" + RESET);
        io.println(CYAN + "║ " + RESET + BOLD + "🎮 BEDIENING MET MUIS OF TOETSENBORD (geen Enter nodig!):" + RESET + CYAN + "           ║" + RESET);
        io.println(CYAN + "║ " + RESET + " • " + YELLOW + BOLD + "Muis Klik" + RESET + " : Klik in willekeurige volgorde op " + YELLOW + BOLD + "1)" + RESET + ".." + YELLOW + BOLD + "8)" + RESET + " of op de " + GREEN + BOLD + "Sliders" + RESET + CYAN + "║" + RESET);
        io.println(CYAN + "║ " + RESET + " • " + YELLOW + BOLD + "Pijltjes" + RESET + "  : " + YELLOW + BOLD + "[↑/↓]" + RESET + " of " + YELLOW + BOLD + "[1-8]" + RESET + " kiest rij " + YELLOW + BOLD + "►" + RESET + " | " + YELLOW + BOLD + "[←/→]" + RESET + " of " + YELLOW + BOLD + "Scrollwiel" + RESET + " schuift" + CYAN + "║" + RESET);
        io.println(CYAN + "║ " + RESET + " • " + CYAN + BOLD + "Font Zoom" + RESET + " : Gebruik " + CYAN + "[Ctrl + Plus/Min]" + RESET + " (of " + CYAN + "[Ctrl+Scroll]" + RESET + " in battle)" + CYAN + "║" + RESET);
        io.println(CYAN + "║ " + RESET + " • " + GREEN + BOLD + "Sluiten" + RESET + "   : Druk " + GREEN + BOLD + "[Enter]/[Esc]/[0]" + RESET + " of klik op " + RED + BOLD + "[✖ SLUITEN: 0]" + RESET + "     " + CYAN + "║" + RESET);
        io.println(CYAN + "╚═════════════════════════════════════════════════════════════════════╝" + RESET);
    }

    /**
     * Opent de tijdelijke Alternate Screen Buffer op positie (0,0) en verbergt de tekstcursor.
     * Zo kloppen de muis-coördinaten (x, y) altijd exact én blijft je gevechts-log op het hoofdscherm bewaard!
     */
    public static void enterSettingsOverlay() {
        System.out.print("\033[?1049h\033[H\033[2J\033[?25l");
        System.out.flush();
    }

    /**
     * Ruimt het Settings Pop-up venster volledig op en schakelt terug naar het originele gevechtsscherm,
     * zodat 100% van alle voorgaande gevechts-logs in de terminal bewaard blijven!
     */
    public static void exitSettingsOverlay() {
        System.out.print("\033[H\033[2J\033[?25h\033[?1049l");
        System.out.flush();
    }

    private static String formatSettingRow(int rowNum, int selectedRow, String label, String valueWithAnsi) {
        boolean isSelected = (rowNum == selectedRow);
        String cursor = isSelected ? (YELLOW + BOLD + "►" + RESET) : " ";
        String numBadge = (isSelected ? YELLOW + BOLD : YELLOW) + rowNum + ")" + RESET;
        String cleanLabel = isSelected ? (BOLD + label + RESET) : label;
        return CYAN + "║ " + RESET + cursor + numBadge + " " + cleanLabel + " : " + padAnsi(valueWithAnsi, 42) + CYAN + "║" + RESET;
    }

    private static String padAnsi(String textWithAnsi, int targetVisibleWidth) {
        String plain = textWithAnsi.replaceAll("\u001B\\[[;\\d]*m", "");
        int pad = Math.max(0, targetVisibleWidth - plain.length());
        return textWithAnsi + " ".repeat(pad);
    }

    /** 2. Retro ASCII-art banner met duidelijke Terminal Shortcuts-balk! */
    public static void printBanner(GameIO io) {
        io.println(YELLOW + BOLD + """
                  ____        _                              ____                 
                 |  _ \\ ___  | | _____ _ __ ___   ___  _ __ / ___|_   _ _ __ ___  
                 | |_) / _ \\ | |/ / _ \\ '_ ` _ \\ / _ \\| '_ \\ |  _| | | | '_ ` _ \\ 
                 |  __/ (_) ||   <  __/ | | | | | (_) | | | | |_| | |_| | | | | | |
                 |_|   \\___/ |_|\\_\\___|_| |_| |_|\\___/|_| |_|\\____|\\__, |_| |_| |_|
                                                                   |___/           
                """ + RESET + CYAN + "         ★ ARCADE & DIVOOM PIXOO MAX EDITION (JLine 4.4.8) ★" + RESET);
        printShortcutsBox(io);
    }

    /** Drukt een overzichtelijk kader af met alle sneltoetsen die overal in het spel werken. */
    public static void printShortcutsBox(GameIO io) {
        io.println(CYAN + "  ┌───────────────────────────────────────────────────────────────────┐" + RESET);
        io.println(CYAN + "  │ " + RESET + BOLD + "TERMINAL SHORTCUTS (werken bij elke vraag in het spel):" + RESET + CYAN + "           │" + RESET);
        io.println(CYAN + "  │ " + RESET + YELLOW + "[s]" + RESET + "            : Open het " + BOLD + "⚙️ Settings Scherm" + RESET + " (Volume, Tempo, Layout)" + CYAN + "│" + RESET);
        io.println(CYAN + "  │ " + RESET + YELLOW + "[m]" + RESET + " of " + YELLOW + "[Alt+M]" + RESET + " : Geluid & Muziek direct AAN / UIT (Mute/Unmute)" + CYAN + "   │" + RESET);
        io.println(CYAN + "  │ " + RESET + YELLOW + "[t]" + RESET + " of " + YELLOW + "[Alt+N]" + RESET + " : Volgende Track (cyclet door alle 5 audio-opties!)" + CYAN + "│" + RESET);
        io.println(CYAN + "  │ " + RESET + YELLOW + "[1-9]" + RESET + " / " + YELLOW + "[Tab]" + RESET + "  : Snel kiezen met nummer, y/n of Tab-autocomplete" + CYAN + "  │" + RESET);
        io.println(CYAN + "  │ " + RESET + YELLOW + "[q]" + RESET + " of " + YELLOW + "[Ctrl+C]" + RESET + ": Spel op elk moment netjes afsluiten" + CYAN + "              │" + RESET);
        io.println(CYAN + "  └───────────────────────────────────────────────────────────────────┘\n" + RESET);
    }

    /**
     * Speelt een echte multi-frame Pokéball animatie af (met suspense wobbles + geluid!)
     * op exact dezelfde plek in de terminal voordat de Pokémon tevoorschijn komt!
     */
    public static void playPokeballAnimation(GameIO io, String trainerColor, String trainerName, Pokemon p) {
        typewriter(io, trainerColor + trainerName + RESET + ": I'll choose you...", textSpeed.getDelayMs() * 2);

        String[] framesColor = {
                // Frame 1: Center Pokéball
                """
                  ⬜⬜⬜⬛⬛⬛⬛⬛⬜⬜⬜
                  ⬜⬜⬛🟥🟥🟥🟥🟥⬛⬜⬜
                  ⬜⬛🟥🟥🟥🟥🟥🟥🟥⬛⬜
                  ⬜⬛⬛⬛⬛⚪⬛⬛⬛⬛⬜
                  ⬜⬛⬜⬜⬜⬛⬜⬜⬜⬛⬜
                  ⬜⬜⬛⬜⬜⬜⬜⬜⬛⬜⬜
                  ⬜⬜⬜⬛⬛⬛⬛⬛⬜⬜⬜
                """,
                // Frame 2: Wobble Left
                """
                ⬜⬜⬜⬛⬛⬛⬛⬛⬜⬜⬜  
                ⬜⬜⬛🟥🟥🟥🟥🟥⬛⬜⬜  
                ⬜⬛🟥🟥🟥🟥🟥🟥🟥⬛⬜  
                ⬜⬛⬛⬛⚪⬛⬛⬛⬛⬛⬜  
                ⬜⬛⬜⬜⬛⬜⬜⬜⬜⬛⬜  
                ⬜⬜⬛⬜⬜⬜⬜⬜⬛⬜⬜  
                ⬜⬜⬜⬛⬛⬛⬛⬛⬜⬜⬜  
                """,
                // Frame 3: Wobble Right
                """
                    ⬜⬜⬜⬛⬛⬛⬛⬛⬜⬜⬜
                    ⬜⬜⬛🟥🟥🟥🟥🟥⬛⬜⬜
                    ⬜⬛🟥🟥🟥🟥🟥🟥🟥⬛⬜
                    ⬜⬛⬛⬛⬛⬛⚪⬛⬛⬛⬜
                    ⬜⬛⬜⬜⬜⬜⬛⬜⬜⬛⬜
                    ⬜⬜⬛⬜⬜⬜⬜⬜⬛⬜⬜
                    ⬜⬜⬜⬛⬛⬛⬛⬛⬜⬜⬜
                """,
                // Frame 4: Burst Open!
                """
                  ✨  ⬜⬛🟥🟥🟥⬛⬜  ✨
                💥  ⬛🟥🟥🟥🟥🟥🟥🟥⬛  💥
                      ✨  ⚡ 💥 ⚡  ✨    
                    ✨  * POOF! *  ✨    
                      ✨  ⚡ 💥 ⚡  ✨    
                💥  ⬛⬜⬜⬜⬜⬜⬜⬜⬛  💥
                  ✨  ⬜⬛⬛⬛⬛⬛⬜  ✨
                """
        };

        String[] framesNormal = {
                // Frame 1: Center
                """
                       .---.   
                      /     \\  
                     |---O---| 
                      \\     /  
                       '---'   
                               
                               
                """,
                // Frame 2: Wobble Left
                """
                     .---.     
                    /     \\    
                   |---O---|   
                    \\     /    
                     '---'     
                               
                               
                """,
                // Frame 3: Wobble Right
                """
                         .---. 
                        /     \\
                       |---O---|
                        \\     /
                         '---' 
                               
                               
                """,
                // Frame 4: Burst Open!
                """
                    \\  .---.  /
                  --  /     \\  --
                     * POOF! * 
                  --  \\     /  --
                    /  '---'  \\
                               
                               
                """
        };

        String[] frames = useColoredArt ? framesColor : framesNormal;
        int frameHeight = 7;

        try {
            SoundEngine.playPokeballWobble();
            io.println(frames[0].stripTrailing());
            Thread.sleep(360);

            System.out.print("\033[" + frameHeight + "A");
            SoundEngine.playPokeballWobble();
            io.println(frames[1].stripTrailing());
            Thread.sleep(360);

            System.out.print("\033[" + frameHeight + "A");
            SoundEngine.playPokeballWobble();
            io.println(frames[2].stripTrailing());
            Thread.sleep(400);

            System.out.print("\033[" + frameHeight + "A");
            SoundEngine.playPokeballOpen();
            io.println(YELLOW + BOLD + frames[3].stripTrailing() + RESET);
            Thread.sleep(420);

            for (int i = 0; i < frameHeight; i++) {
                System.out.print("\033[1A\033[2K");
            }
            System.out.flush();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        io.println(trainerColor + BOLD + trainerName + ": GO, " + p.getName().toUpperCase() + "!!!" + RESET);
        printPokemonAscii(io, p);
    }

    public static void printPokemonAscii(GameIO io, Pokemon p) {
        String baseName = resolveAssetName(p.getName());
        String fileName = useColoredArt ? baseName + "-color.txt" : baseName + ".txt";
        Path filePath = ASCII_DIR.resolve(fileName);

        if (!Files.exists(filePath)) {
            filePath = ASCII_DIR.resolve(baseName + ".txt");
        }

        String color = switch (p.getPokemonType()) {
            case FIRE -> RED;
            case WATER -> BLUE;
            case GRASS -> GREEN;
            case ELECTRIC -> YELLOW;
        };

        String icon = switch (p.getPokemonType()) {
            case FIRE -> "🔥";
            case WATER -> "💧";
            case GRASS -> "🍃";
            case ELECTRIC -> "⚡";
        };

        try {
            List<String> lines = Files.readAllLines(filePath);
            String alignedArt = alignInfoNextToArt(lines,
                    color + BOLD + icon + " " + p.getName().toUpperCase() + "!" + RESET,
                    color + "   \"" + p.getSound().trim() + "\"" + RESET,
                    "   Level " + p.getLevel() + " | " + p.getHp() + "/" + p.getMaxHp() + " HP",
                    useColoredArt ? "" : color);
            io.println(alignedArt);
        } catch (IOException e) {
            io.println(color + BOLD + icon + " " + p.getName().toUpperCase() + "! \"" + p.getSound() + "\"" + RESET);
        }
    }

    private static String alignInfoNextToArt(List<String> artLines, String titleLine, String soundLine, String statsLine, String artColor) {
        int maxVisualWidth = 0;
        for (String line : artLines) {
            maxVisualWidth = Math.max(maxVisualWidth, visualLength(line));
        }

        int mid = artLines.size() / 2;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < artLines.size(); i++) {
            String rawLine = artLines.get(i);
            int currentWidth = visualLength(rawLine);
            int spacesNeeded = Math.max(2, (maxVisualWidth - currentWidth) + 4);
            String padding = " ".repeat(spacesNeeded);

            sb.append(artColor).append(rawLine).append(RESET);

            if (i == mid - 1) {
                sb.append(padding).append(titleLine);
            } else if (i == mid) {
                sb.append(padding).append(soundLine);
            } else if (i == mid + 1) {
                sb.append(padding).append(statsLine);
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    private static int visualLength(String line) {
        int width = 0;
        for (int i = 0; i < line.length(); ) {
            int cp = line.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == 0xFE0F) {
                continue;
            }
            if (cp >= 0x1F300 || cp == 0x2B1B || cp == 0x2B1C) {
                width += 2;
            } else {
                width += 1;
            }
        }
        return width;
    }

    private static String resolveAssetName(String pokemonName) {
        String lower = pokemonName.toLowerCase();
        if (lower.contains("char")) return "Charizard";
        if (lower.contains("blast")) return "Blastoise";
        if (lower.contains("venu")) return "Venusaur";
        if (lower.contains("rai")) return "Raichu";
        if (lower.contains("gya")) return "Gyarados";
        return "Ditto";
    }

    public static void syncDivoomState(String event, Pokemon playerPokemon, Pokemon gymPokemon, String lastAttack) {
        String playerAsset = playerPokemon != null ? resolveAssetName(playerPokemon.getName()) : "None";
        String gymAsset = gymPokemon != null ? resolveAssetName(gymPokemon.getName()) : "None";
        String json = String.format("""
                {
                  "timestamp": "%s",
                  "event": "%s",
                  "artMode": "%s",
                  "lastAttack": "%s",
                  "playerPokemon": {
                    "name": "%s",
                    "assetFile": "assets/ASCII/%s-color.txt",
                    "type": "%s",
                    "hp": %d,
                    "maxHp": %d
                  },
                  "gymPokemon": {
                    "name": "%s",
                    "assetFile": "assets/ASCII/%s-color.txt",
                    "type": "%s",
                    "hp": %d,
                    "maxHp": %d
                  }
                }
                """,
                Instant.now().toString(),
                event,
                useColoredArt ? "color" : "normal",
                lastAttack != null ? lastAttack : "",
                playerPokemon != null ? playerPokemon.getName() : "",
                playerAsset,
                playerPokemon != null ? playerPokemon.getType() : "",
                playerPokemon != null ? playerPokemon.getHp() : 0,
                playerPokemon != null ? playerPokemon.getMaxHp() : 0,
                gymPokemon != null ? gymPokemon.getName() : "",
                gymAsset,
                gymPokemon != null ? gymPokemon.getType() : "",
                gymPokemon != null ? gymPokemon.getHp() : 0,
                gymPokemon != null ? gymPokemon.getMaxHp() : 0
        );
        try {
            Files.writeString(DIVOOM_STATE_FILE, json);
        } catch (IOException ignored) {
        }
    }

    /** 3. Typewriter-effect: gebruikt de ingestelde TextSpeed uit het Settings-scherm! */
    public static void typewriter(GameIO io, String text, int ignoredDefaultDelay) {
        int activeDelay = textSpeed.getDelayMs();
        if (activeDelay <= 0) {
            io.println(text);
            return;
        }
        for (char c : text.toCharArray()) {
            System.out.print(c);
            System.out.flush();
            if (c != '\u001B') {
                try {
                    Thread.sleep(activeDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        System.out.println();
    }

    public static void clearScreen(GameIO io) {
        io.println("\033[H\033[2J");
    }

    public static void soundBell() {
        new Thread(() -> {
            playChiptuneTone(523, 70);
            playChiptuneTone(659, 70);
            playChiptuneTone(784, 120);
        }).start();
    }

    public static void soundVictory() {
        new Thread(() -> {
            playChiptuneTone(523, 90);
            playChiptuneTone(659, 90);
            playChiptuneTone(784, 90);
            playChiptuneTone(1046, 220);
        }).start();
    }

    private static void playChiptuneTone(int hz, int durationMs) {
        try {
            float sampleRate = 16000f;
            byte[] buf = new byte[(int) (sampleRate * durationMs / 1000)];
            int period = (int) (sampleRate / hz);
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (i % period < period / 2) ? (byte) 35 : (byte) -35;
            }
            AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
            try (SourceDataLine sdl = AudioSystem.getSourceDataLine(af)) {
                sdl.open(af);
                sdl.start();
                sdl.write(buf, 0, buf.length);
                sdl.drain();
            }
        } catch (Exception ignored) {
        }
    }

    public static Map<String, Integer> loadHighscores() {
        Map<String, Integer> scores = new HashMap<>();
        if (!Files.exists(SAVE_FILE)) {
            return scores;
        }
        try {
            List<String> lines = Files.readAllLines(SAVE_FILE);
            for (String line : lines) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    scores.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                }
            }
        } catch (IOException | NumberFormatException ignored) {
        }
        return scores;
    }

    public static int getPlayerBadges(String playerName) {
        return loadHighscores().getOrDefault(playerName, 0);
    }

    public static int incrementPlayerBadges(String playerName) {
        Map<String, Integer> scores = loadHighscores();
        int newScore = scores.getOrDefault(playerName, 0) + 1;
        scores.put(playerName, newScore);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(System.lineSeparator());
        }
        try {
            Files.writeString(SAVE_FILE, sb.toString());
        } catch (IOException ignored) {
        }
        return newScore;
    }
}
