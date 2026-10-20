package io;

import ui.ArcadeUI;

import java.util.List;

/**
 * Slimme Keuze-Helper voor de Terminal!
 *
 * Zorgt ervoor dat de speler bij ELKE meerkeuzevraag gewoon kan typen:
 * - 1, 2, 3 ... t/m 9 (of 1), 2), 3))
 * - Vanaf optie 10 telt hij automatisch door met letters: a, b, c, d... (voor future use!)
 * - Bij yes/no vragen werkt ook gewoon 'y' of 'n' (en 1 of 2).
 * - Volledige namen typen (of [Tab] autocomplete gebruiken) blijft natuurlijk ook werken!
 */
public final class ChoiceHelper {

    private ChoiceHelper() {
    }

    /**
     * Geeft het keuzelabel voor een 0-based index terug:
     * Index 0..8  -> "1" t/m "9"
     * Index 9..34 -> "a" t/m "z"
     */
    public static String getKeyForIndex(int index) {
        if (index < 0) {
            return "";
        }
        if (index < 9) {
            return String.valueOf(index + 1);
        }
        char letter = (char) ('a' + (index - 9));
        return String.valueOf(letter);
    }

    /** Geeft bijv. "1)", "2)", ... "9)", "a)", "b)" terug. */
    public static String getBadgeForIndex(int index) {
        return ArcadeUI.YELLOW + ArcadeUI.BOLD + getKeyForIndex(index) + ")" + ArcadeUI.RESET;
    }

    /**
     * Formatteert een lijst opties op één overzichtelijke regel:
     * bijv. "1) inferno   2) pyroBall   3) fireLash   4) flameThrower"
     */
    public static String formatInlineOptions(List<String> options) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) {
                sb.append("   ");
            }
            sb.append(getBadgeForIndex(i)).append(" ").append(options.get(i));
        }
        return sb.toString();
    }

    /**
     * Vertaalt de invoer van de gebruiker (bijv. "1", "1)", "a", "y", "n" of de volledige tekst)
     * automatisch naar de bijbehorende optie uit de lijst!
     * Geeft de originele invoer terug als die nergens bij hoort (zodat de validatielus een melding kan tonen).
     */
    public static String resolveInput(String rawInput, List<String> options) {
        if (rawInput == null || options == null || options.isEmpty()) {
            return rawInput == null ? "" : rawInput;
        }
        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        // 1. Speciale ondersteuning voor Yes/No vragen: 'y'/'j'/'1' -> "yes", 'n'/'2' -> "no"
        if (isYesNoOptions(options)) {
            if (trimmed.equalsIgnoreCase("y")
                    || trimmed.equalsIgnoreCase("yes")
                    || trimmed.equalsIgnoreCase("j")
                    || trimmed.equalsIgnoreCase("ja")
                    || trimmed.equals("1")
                    || trimmed.equals("1)")) {
                return "yes";
            }
            if (trimmed.equalsIgnoreCase("n")
                    || trimmed.equalsIgnoreCase("no")
                    || trimmed.equalsIgnoreCase("nee")
                    || trimmed.equals("2")
                    || trimmed.equals("2)")) {
                return "no";
            }
        }

        // 2. Controleer eerst of de speler de exacte optienaam heeft getypt (bijv. "color" of "a")
        for (String opt : options) {
            if (opt.equalsIgnoreCase(trimmed)) {
                return opt;
            }
        }

        // 3. Strip eventuele ')' of '.' achter het nummer/letter (zodat zowel "1" als "1)" en "1." werken)
        String cleanKey = trimmed;
        if (cleanKey.endsWith(")") || cleanKey.endsWith(".")) {
            cleanKey = cleanKey.substring(0, cleanKey.length() - 1).trim();
        }

        // 4. Vergelijk met onze automatische index-sleutels: 1..9 en vanaf het 10e item a..z!
        for (int i = 0; i < options.size(); i++) {
            if (getKeyForIndex(i).equalsIgnoreCase(cleanKey)) {
                return options.get(i);
            }
        }

        return trimmed;
    }

    private static boolean isYesNoOptions(List<String> options) {
        return options.size() == 2
                && ((options.get(0).equalsIgnoreCase("yes") && options.get(1).equalsIgnoreCase("no"))
                || (options.get(0).equalsIgnoreCase("y") && options.get(1).equalsIgnoreCase("n")));
    }
}
