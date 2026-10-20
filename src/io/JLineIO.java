package io;

import audio.SoundEngine;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Attributes;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import ui.ArcadeUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JLineIO implements GameIO, AutoCloseable {
    private final Terminal terminal;
    private final LineReader reader;
    private final List<String> currentOptions = new ArrayList<>();

    public JLineIO() throws IOException {
        terminal = TerminalBuilder.builder()
                .system(true)
                .dumb(true)
                .build();
        reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter(() -> currentOptions))
                .option(LineReader.Option.AUTO_MENU, true)
                .option(LineReader.Option.CASE_INSENSITIVE, true)
                .option(LineReader.Option.COMPLETE_IN_WORD, true)
                .build();

        // Standaard staat MouseTracking UIT zodat [Ctrl + Scrollwiel] altijd je Terminal Font kan zoomen
        // en klikken in het terminal-venster nooit je gevecht verstoort!
        terminal.trackMouse(Terminal.MouseTracking.Off);

        // 1. Bind Alt+M als directe sneltoets (hotkey) om muziek & geluid overal aan/uit te zetten!
        Widget toggleMusicWidget = () -> {
            boolean soundOn = SoundEngine.toggleMute();
            if (soundOn) {
                reader.printAbove(ArcadeUI.GREEN + "🔊 [Alt+M] 8-Bit Audio & Music: ON (" + SoundEngine.getMusicMode().getLabel() + ")" + ArcadeUI.RESET);
            } else {
                reader.printAbove(ArcadeUI.YELLOW + "🔇 [Alt+M] 8-Bit Audio & Music: MUTED" + ArcadeUI.RESET);
            }
            return true;
        };
        reader.getWidgets().put("toggle-music", toggleMusicWidget);
        reader.getKeyMaps().get(LineReader.MAIN).bind(new Reference("toggle-music"), KeyMap.alt('m'), KeyMap.alt('M'));

        // 2. Bind Alt+N en Alt+T als directe sneltoets om door alle 5 de soundtracks/audio-modes te cyclen!
        Widget nextTrackWidget = () -> {
            SoundEngine.MusicMode next = SoundEngine.cycleNextTrack();
            int idx = next.ordinal() + 1;
            int total = SoundEngine.MusicMode.values().length;
            reader.printAbove(ArcadeUI.CYAN + "🎵 [Alt+N] Next Track [" + idx + "/" + total + "]: " + ArcadeUI.BOLD + next.getLabel() + ArcadeUI.RESET);
            return true;
        };
        reader.getWidgets().put("next-track", nextTrackWidget);
        reader.getKeyMaps().get(LineReader.MAIN).bind(
                new Reference("next-track"),
                KeyMap.alt('n'), KeyMap.alt('N'),
                KeyMap.alt('t'), KeyMap.alt('T')
        );
    }

    @Override
    public void println(String text) {
        terminal.writer().println(text);
        terminal.flush();
    }

    @Override
    public String readLine(String prompt) {
        return readLine(prompt, null);
    }

    /**
     * Past de geselecteerde rij (1..8) in het Settings Dashboard aan met richting delta (-1 = links/omlaag, +1 = rechts/omhoog).
     */
    private void adjustSettingRow(int selectedRow, int delta) {
        switch (selectedRow) {
            case 1 -> SoundEngine.toggleMute();
            case 2 -> SoundEngine.cycleTrack(delta);
            case 3 -> SoundEngine.adjustVolume(delta * 5);
            case 4 -> SoundEngine.adjustTempo(delta * 0.05);
            case 5 -> ArcadeUI.setUseColoredArt(!ArcadeUI.isUseColoredArt());
            case 6 -> ArcadeUI.cycleLineSpacing(delta);
            case 7 -> ArcadeUI.cycleTextSpeed(delta);
            case 8 -> {
                ArcadeUI.resetDefaults();
                SoundEngine.playHealJingle();
            }
        }
    }

    /**
     * Opent het dedicated In-Place Settings Pop-up Venster (via 's' of 'settings').
     * Gebruikt de Alternate Screen Buffer op positie (0,0) zodat:
     * 1. Bij het sluiten 100% van al je voorgaande gevechts-logs op het scherm bewaard blijven.
     * 2. Je met de muis in willekeurige volgorde op 1) t/m 8) kunt klikken om opties te cyclen.
     * 3. Je direct op een plek op de Volume-slider (rij 3) of Tempo-slider (rij 4) kunt klikken!
     */
    private void openSettingsScreen() {
        int selectedRow = 3; // Start standaard op rij 3: Muziek Volume
        boolean isFirstDraw = true;

        // Wis eerst de "Choice: s" invoerregel zodat die niet in je gevechts-log blijft staan!
        System.out.print("\033[1A\033[2K\r");
        ArcadeUI.enterSettingsOverlay();
        terminal.trackMouse(Terminal.MouseTracking.Normal);

        KeyMap<String> keyMap = buildSettingsKeyMap();
        Attributes savedAttributes = terminal.enterRawMode();
        BindingReader bindingReader = new BindingReader(terminal.reader());
        MouseEvent lastMouseEvent = null;

        try {
            while (true) {
                ArcadeUI.renderSettingsDashboard(this, selectedRow, isFirstDraw);
                isFirstDraw = false;

                String action = bindingReader.readBinding(keyMap);
                if (action == null || action.equals("EXIT")) {
                    break;
                }
                if (action.equals("QUIT_GAME")) {
                    SoundEngine.stopBattleMusic();
                    throw new GameQuitException();
                }

                switch (action) {
                    case "UP" -> selectedRow = (selectedRow <= 1) ? 8 : selectedRow - 1;
                    case "DOWN" -> selectedRow = (selectedRow >= 8) ? 1 : selectedRow + 1;
                    case "LEFT" -> adjustSettingRow(selectedRow, -1);
                    case "RIGHT", "TOGGLE" -> adjustSettingRow(selectedRow, +1);
                    case "ROW_1" -> {
                        selectedRow = 1;
                        SoundEngine.toggleMute();
                    }
                    case "ROW_2" -> {
                        selectedRow = 2;
                        SoundEngine.cycleTrack(+1);
                    }
                    case "ROW_3" -> {
                        if (selectedRow == 3) {
                            SoundEngine.adjustVolume(+10);
                        }
                        selectedRow = 3;
                    }
                    case "ROW_4" -> {
                        if (selectedRow == 4) {
                            SoundEngine.adjustTempo(+0.10);
                        }
                        selectedRow = 4;
                    }
                    case "ROW_5" -> {
                        selectedRow = 5;
                        ArcadeUI.setUseColoredArt(!ArcadeUI.isUseColoredArt());
                    }
                    case "ROW_6" -> {
                        selectedRow = 6;
                        ArcadeUI.cycleLineSpacing(+1);
                    }
                    case "ROW_7" -> {
                        selectedRow = 7;
                        ArcadeUI.cycleTextSpeed(+1);
                    }
                    case "ROW_8" -> {
                        selectedRow = 8;
                        adjustSettingRow(8, +1);
                    }
                    case "MOUSE" -> {
                        MouseEvent event = terminal.readMouseEvent(bindingReader::readCharacter, bindingReader.getLastBinding());
                        if (event == null) {
                            break;
                        }
                        lastMouseEvent = event;
                        int x = event.getX();
                        int y = event.getY();

                        if (event.getType() == MouseEvent.Type.Wheel) {
                            // Als je muis boven een specifieke instelling-rij (y = 3..9 -> rij 1..7) zweeft,
                            // selecteer en scroll die rij! Anders scroll de rij waar de gele pijl ► op staat.
                            int hoveredRow = y - 2;
                            if (hoveredRow >= 1 && hoveredRow <= 7) {
                                selectedRow = hoveredRow;
                            }
                            if (event.getButton() == MouseEvent.Button.WheelUp) {
                                adjustSettingRow(selectedRow, +1);
                            } else if (event.getButton() == MouseEvent.Button.WheelDown) {
                                adjustSettingRow(selectedRow, -1);
                            }
                        } else if ((event.getType() == MouseEvent.Type.Pressed || event.getType() == MouseEvent.Type.Dragged)
                                && event.getButton() == MouseEvent.Button.Button1) {
                            // Klik op "[✖ SLUITEN: 0]" rechtsboven (y == 1, x >= 50) of onderaan op rij 16 (Sluiten)
                            if ((y == 1 && x >= 50) || y == 16) {
                                return;
                            }

                            // Rijen 1) t/m 8) staan altijd vast op y = 3 t/m 10 (omdat de box op (0,0) begint!)
                            int clickedRow = y - 2;
                            if (clickedRow >= 1 && clickedRow <= 8) {
                                boolean wasAlreadySelected = (selectedRow == clickedRow);
                                selectedRow = clickedRow;

                                if (clickedRow == 3 || clickedRow == 4) {
                                    // Rij 3 = Muziek Volume slider, Rij 4 = Muziek Tempo slider
                                    // Opbouw rij: "║ ►3) 🎚️ Muziek Volume   : [-] [████████████░░░░░] [+]  70%           ║"
                                    // Kolom 27..31 = [-], Kolom 32..50 = [slider balk], Kolom 51..56 = [+]
                                    if (x >= 27 && x <= 31) {
                                        adjustSettingRow(clickedRow, -2);
                                    } else if (x >= 32 && x <= 50) {
                                        double ratio = Math.max(0.0, Math.min(1.0, (x - 33.0) / (SoundEngine.SLIDER_WIDTH - 1.0)));
                                        if (clickedRow == 3) {
                                            int targetVol = (int) Math.round(SoundEngine.MIN_VOLUME + ratio * (SoundEngine.MAX_VOLUME - SoundEngine.MIN_VOLUME));
                                            SoundEngine.setVolumePercent(targetVol);
                                        } else {
                                            double targetTempo = SoundEngine.MIN_TEMPO + ratio * (SoundEngine.MAX_TEMPO - SoundEngine.MIN_TEMPO);
                                            SoundEngine.setTempoMultiplier(targetTempo);
                                        }
                                    } else if (x >= 51 && x <= 56) {
                                        adjustSettingRow(clickedRow, +2);
                                    } else if (event.getType() == MouseEvent.Type.Pressed) {
                                        // Klik vooraan op "3)" of "4)" selecteert de rij (en cyclet als hij al geselecteerd was)
                                        if (wasAlreadySelected) {
                                            if (clickedRow == 3) {
                                                int nextVol = (SoundEngine.getVolumePercent() >= 100) ? 20 : SoundEngine.getVolumePercent() + 10;
                                                SoundEngine.setVolumePercent(nextVol);
                                            } else {
                                                double nextTempo = (SoundEngine.getTempoMultiplier() >= 2.0) ? 0.50 : SoundEngine.getTempoMultiplier() + 0.25;
                                                SoundEngine.setTempoMultiplier(nextTempo);
                                            }
                                        } else {
                                            SoundEngine.playSelectBeep();
                                        }
                                    }
                                } else if (event.getType() == MouseEvent.Type.Pressed) {
                                    // Klikken op 1), 2), 5), 6), 7) of 8) in willekeurige volgorde schakelt/cyclet direct!
                                    adjustSettingRow(clickedRow, +1);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            terminal.trackMouse(Terminal.MouseTracking.Off);
            terminal.setAttributes(savedAttributes);
            ArcadeUI.exitSettingsOverlay();
        }
    }

    private KeyMap<String> buildSettingsKeyMap() {
        KeyMap<String> km = new KeyMap<>();
        km.setAmbiguousTimeout(150L);

        // Pijltjestoetsen (ANSI + Windows/VT100 + Terminfo)
        km.bind("UP", "\033[A", "\033OA", "k", "w", "W");
        km.bind("DOWN", "\033[B", "\033OB", "j", "J");
        km.bind("RIGHT", "\033[C", "\033OC", "+", "=", ".", ">", "d", "D");
        km.bind("LEFT", "\033[D", "\033OD", "-", "_", ",", "<", "a", "A");

        String termUp = KeyMap.key(terminal, InfoCmp.Capability.key_up);
        String termDown = KeyMap.key(terminal, InfoCmp.Capability.key_down);
        String termRight = KeyMap.key(terminal, InfoCmp.Capability.key_right);
        String termLeft = KeyMap.key(terminal, InfoCmp.Capability.key_left);
        String termMouse = KeyMap.key(terminal, InfoCmp.Capability.key_mouse);
        if (termUp != null) km.bind("UP", termUp);
        if (termDown != null) km.bind("DOWN", termDown);
        if (termRight != null) km.bind("RIGHT", termRight);
        if (termLeft != null) km.bind("LEFT", termLeft);
        if (termMouse != null) km.bind("MOUSE", termMouse);
        km.bind("MOUSE", "\033[M", "\033[<");

        // Directe rij-selectie 1 t/m 8
        km.bind("ROW_1", "1", "m", "M");
        km.bind("ROW_2", "2", "t", "T");
        km.bind("ROW_3", "3", "v", "V");
        km.bind("ROW_4", "4");
        km.bind("ROW_5", "5");
        km.bind("ROW_6", "6");
        km.bind("ROW_7", "7");
        km.bind("ROW_8", "8", "r", "R");

        // Spatiebalk schakelt de geselecteerde rij
        km.bind("TOGGLE", " ");

        // Sluit Settings scherm (Enter, Esc, 0, s, b)
        km.bind("EXIT", "\r", "\n", "\033", "0", "s", "S", "b", "B");
        km.bind("QUIT_GAME", "q", "Q", KeyMap.ctrl('C'));
        return km;
    }

    @Override
    public String readLine(String prompt, List<String> options) {
        while (true) {
            currentOptions.clear();
            if (options != null && !options.isEmpty()) {
                currentOptions.addAll(options);
                for (int i = 0; i < options.size(); i++) {
                    currentOptions.add(ChoiceHelper.getKeyForIndex(i));
                }
            }
            currentOptions.add("s"); // Sneltoets voor Settings scherm
            currentOptions.add("m"); // Sneltoets voor mute/unmute
            currentOptions.add("t"); // Sneltoets voor next track
            currentOptions.add("q"); // Sneltoets voor quit
            try {
                String line = reader.readLine(prompt + " ");
                String trimmed = line != null ? line.trim() : "";

                // Globaal commando 'q' of 'quit' om het spel netjes af te sluiten
                if (trimmed.equalsIgnoreCase("q") || trimmed.equalsIgnoreCase("quit")) {
                    SoundEngine.stopBattleMusic();
                    throw new GameQuitException();
                }

                // Globaal commando 's' of 'settings' om het In-Place Settings Scherm te openen!
                if (trimmed.equalsIgnoreCase("s") || trimmed.equalsIgnoreCase("settings")) {
                    openSettingsScreen();
                    continue;
                }

                // Globaal commando 'm' of 'mute' om het geluid op elk moment aan/uit te zetten
                if (trimmed.equalsIgnoreCase("m") || trimmed.equalsIgnoreCase("mute")) {
                    boolean soundOn = SoundEngine.toggleMute();
                    if (soundOn) {
                        println(ArcadeUI.GREEN + "🔊 8-Bit Audio & Music: ON (" + SoundEngine.getMusicMode().getLabel() + ")" + ArcadeUI.RESET);
                    } else {
                        println(ArcadeUI.YELLOW + "🔇 8-Bit Audio & Music: MUTED (Tip: typ 'm' of druk op Alt+M om weer aan te zetten)" + ArcadeUI.RESET);
                    }
                    continue;
                }

                // Globaal commando 't' of 'next' om door alle 5 de soundtracks/audio-modes te cyclen!
                if (trimmed.equalsIgnoreCase("t") || trimmed.equalsIgnoreCase("next")) {
                    SoundEngine.MusicMode next = SoundEngine.cycleNextTrack();
                    int idx = next.ordinal() + 1;
                    int total = SoundEngine.MusicMode.values().length;
                    println(ArcadeUI.CYAN + "🎵 Next Track [" + idx + "/" + total + "]: " + ArcadeUI.BOLD + next.getLabel()
                            + ArcadeUI.RESET + ArcadeUI.CYAN + " (Tip: typ 's' voor het volledige Settings Scherm!)" + ArcadeUI.RESET);
                    continue;
                }

                SoundEngine.playSelectBeep();
                // Vertaal automatisch '1', '2', '1)', 'a', 'b', 'y', 'n' naar de gekozen optie!
                return ChoiceHelper.resolveInput(trimmed, options);
            } catch (UserInterruptException | EndOfFileException e) {
                SoundEngine.stopBattleMusic();
                throw new GameQuitException();
            } finally {
                currentOptions.clear();
            }
        }
    }

    @Override
    public void close() throws IOException {
        SoundEngine.stopBattleMusic();
        try {
            terminal.trackMouse(Terminal.MouseTracking.Off);
        } catch (Exception ignored) {
        }
        terminal.close();
    }
}
