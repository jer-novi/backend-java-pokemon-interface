package audio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BEWAARDE HANDMATIGE JSON-PARSER (VOOR STUDIEDOELEINDEN).
 *
 * Wordt nu NIET meer gebruikt door het spel (we gebruiken nu Jackson in ChiptuneJsonSynth.java!),
 * maar is hier bewaard zodat je later rustig kunt bekijken hoe een handgebouwde
 * parser met accolade-tellen (depth tracking) en Regex werkt.
 */
public final class ManualJsonParser {

    private ManualJsonParser() {
    }

    public static class ChannelData {
        public boolean isDrum;
        public List<Integer> sequence = new ArrayList<>();
        public List<List<NoteData>> patterns = new ArrayList<>();
    }

    public static class NoteData {
        public List<Integer> pitches = new ArrayList<>();
        public List<PointData> points = new ArrayList<>();
    }

    public record PointData(int tick, int volume) {
    }

    /**
     * Voorbeeld van hoe je zonder externe libraries een JSON-bestand kunt doorlopen
     * door openende '{' / '[' en sluitende '}' / ']' haakjes te tellen.
     */
    public static List<ChannelData> parseChannelsManually(Path jsonPath) throws Exception {
        String json = Files.readString(jsonPath);
        List<ChannelData> result = new ArrayList<>();

        int channelsStart = json.indexOf("\"channels\"");
        if (channelsStart < 0) return result;
        int arrStart = json.indexOf('[', channelsStart);
        int arrEnd = findMatchingBracket(json, arrStart, '[', ']');
        String channelsArrayStr = json.substring(arrStart + 1, arrEnd);

        for (String chBlock : splitTopLevelObjects(channelsArrayStr)) {
            ChannelData ch = new ChannelData();
            ch.isDrum = chBlock.contains("\"type\": \"drum\"") || chBlock.contains("\"type\":\"drum\"");

            // 1. Parse "sequence": [ ... ]
            int seqIdx = chBlock.indexOf("\"sequence\"");
            if (seqIdx >= 0) {
                int sStart = chBlock.indexOf('[', seqIdx);
                int sEnd = findMatchingBracket(chBlock, sStart, '[', ']');
                ch.sequence = parseIntList(chBlock.substring(sStart + 1, sEnd));
            }

            // 2. Parse "patterns": [ ... ]
            int patIdx = chBlock.indexOf("\"patterns\"");
            if (patIdx >= 0) {
                int pStart = chBlock.indexOf('[', patIdx);
                int pEnd = findMatchingBracket(chBlock, pStart, '[', ']');
                String patArrayStr = chBlock.substring(pStart + 1, pEnd);
                for (String patBlock : splitTopLevelObjects(patArrayStr)) {
                    ch.patterns.add(parseNotes(patBlock));
                }
            }
            result.add(ch);
        }
        return result;
    }

    private static List<NoteData> parseNotes(String patBlock) {
        List<NoteData> notes = new ArrayList<>();
        int notesIdx = patBlock.indexOf("\"notes\"");
        if (notesIdx < 0) return notes;
        int nStart = patBlock.indexOf('[', notesIdx);
        int nEnd = findMatchingBracket(patBlock, nStart, '[', ']');
        String notesArrayStr = patBlock.substring(nStart + 1, nEnd);

        for (String noteBlock : splitTopLevelObjects(notesArrayStr)) {
            NoteData nd = new NoteData();
            int pIdx = noteBlock.indexOf("\"pitches\"");
            if (pIdx >= 0) {
                int pStart = noteBlock.indexOf('[', pIdx);
                int pEnd = findMatchingBracket(noteBlock, pStart, '[', ']');
                nd.pitches = parseIntList(noteBlock.substring(pStart + 1, pEnd));
            }
            int ptIdx = noteBlock.indexOf("\"points\"");
            if (ptIdx >= 0) {
                int ptStart = noteBlock.indexOf('[', ptIdx);
                int ptEnd = findMatchingBracket(noteBlock, ptStart, '[', ']');
                for (String ptObj : splitTopLevelObjects(noteBlock.substring(ptStart + 1, ptEnd))) {
                    int tick = extractInt(ptObj, "\"tick\"\\s*:\\s*(\\d+)", 0);
                    int vol = extractInt(ptObj, "\"volume\"\\s*:\\s*(\\d+)", 100);
                    nd.points.add(new PointData(tick, vol));
                }
            }
            notes.add(nd);
        }
        return notes;
    }

    /**
     * Splitst een JSON-array-string in losse '{ ... }' blokken op het bovenste niveau (depth == 0).
     */
    private static List<String> splitTopLevelObjects(String str) {
        List<String> list = new ArrayList<>();
        int depth = 0;
        int objStart = -1;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    list.add(str.substring(objStart, i + 1));
                    objStart = -1;
                }
            }
        }
        return list;
    }

    /**
     * Zoekt het bijbehorende sluit-haakje ']' of '}' door bij elk open-haakje depth++
     * en bij elk sluit-haakje depth-- te doen.
     */
    private static int findMatchingBracket(String s, int openPos, char openChar, char closeChar) {
        int depth = 0;
        for (int i = openPos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == openChar) depth++;
            else if (c == closeChar) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return s.length() - 1;
    }

    private static List<Integer> parseIntList(String s) {
        List<Integer> list = new ArrayList<>();
        Matcher m = Pattern.compile("-?\\d+").matcher(s);
        while (m.find()) {
            list.add(Integer.parseInt(m.group()));
        }
        return list;
    }

    public static int extractInt(String s, String regex, int fallback) {
        Matcher m = Pattern.compile(regex).matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : fallback;
    }
}
