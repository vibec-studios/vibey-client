package dev.henny.vui.headless;

import java.util.HashMap;
import java.util.Map;

public final class PixelFont {
    public static final int WIDTH = 5;
    public static final int HEIGHT = 7;
    public static final int ADVANCE = 6;
    public static final int LINE_HEIGHT = 9;

    private static final String[] ASCII = {
            "00000000000000", "04040404000004", "0A0A0A00000000", "0A0A1F0A1F0A0A", "040F140E051E04",
            "18190204081303", "0C12140815120D", "0C040800000000", "02040808080402", "08040202020408",
            "0004150E150400", "0004041F040400", "000000000C0408", "0000001F000000", "00000000000C0C",
            "00010204081000", "0E11131519110E", "040C040404040E", "0E11010204081F", "1F02040201110E",
            "02060A121F0202", "1F101E0101110E", "0608101E11110E", "1F010204080808", "0E11110E11110E",
            "0E11110F01020C", "000C0C000C0C00", "000C0C000C0408", "02040810080402", "00001F001F0000",
            "08040201020408", "0E110102040004", "0E11010D15150E", "0E1111111F1111", "1E11111E11111E",
            "0E11101010110E", "1C12111111121C", "1F10101E10101F", "1F10101E101010", "0E11101711110F",
            "1111111F111111", "0E04040404040E", "0702020202120C", "11121418141211", "1010101010101F",
            "111B1515111111", "11111915131111", "0E11111111110E", "1E11111E101010", "0E11111115120D",
            "1E11111E141211", "0F10100E01011E", "1F040404040404", "1111111111110E", "11111111110A04",
            "1111111515150A", "11110A040A1111", "1111110A040404", "1F01020408101F", "0E08080808080E",
            "00100804020100", "0E02020202020E", "040A1100000000", "0000000000001F", "08040200000000",
            "00000E010F110F", "1010161911111E", "00000E1010110E", "01010D1311110F", "00000E111F100E",
            "0609081C080808", "000F11110F010E", "10101619111111", "04000C0404040E", "0200060202120C",
            "10101214181412", "0C04040404040E", "00001A15151111", "00001619111111", "00000E1111110E",
            "00001E111E1010", "00000D130F0101", "00001619101010", "00000E100E011E", "08081C08080906",
            "0000111111130D", "00001111110A04", "0000111115150A", "0000110A040A11", "000011110F010E",
            "00001F0204081F", "02040408040402", "04040404040404", "08040402040408", "00000815020000",
    };

    private static final Map<Integer, int[]> EXTRA = new HashMap<>();

    static {
        EXTRA.put((int) '…', rows("00000000000015"));
        EXTRA.put((int) '•', rows("00000E0E0E0000"));
        EXTRA.put((int) '—', rows("0000001F000000"));
        EXTRA.put((int) '→', rows("0004021F020400"));
        EXTRA.put((int) '←', rows("0004081F080400"));
        EXTRA.put((int) '✓', rows("00010102141408"));
        EXTRA.put((int) '×', rows("00110A040A1100"));
        EXTRA.put((int) '›', rows("00080402040800"));
        EXTRA.put((int) '‹', rows("00020408040200"));
        EXTRA.put((int) '·', rows("00000004000000"));
        EXTRA.put((int) 'ä', rows("0A000E010F110F"));
        EXTRA.put((int) 'ö', rows("0A000E1111110E"));
        EXTRA.put((int) 'ü', rows("0A00111111130D"));
        EXTRA.put((int) 'Ä', rows("0A0E11111F1111"));
        EXTRA.put((int) 'Ö', rows("0A0E111111110E"));
        EXTRA.put((int) 'Ü', rows("0A11111111110E"));
        EXTRA.put((int) 'ß', rows("0C12121C121216"));
    }

    private PixelFont() {
    }

    public static int[] glyph(int codepoint) {
        if (codepoint >= 32 && codepoint < 127) return rows(ASCII[codepoint - 32]);
        int[] extra = EXTRA.get(codepoint);
        if (extra != null) return extra;
        if (Character.isLetter(codepoint)) {
            int base = Character.toUpperCase(stripAccent(codepoint));
            if (base >= 'A' && base <= 'Z') return glyph(base);
        }
        return null;
    }

    public static boolean pixel(int[] rows, int x, int y) {
        return (rows[y] >> (WIDTH - 1 - x) & 1) != 0;
    }

    private static int stripAccent(int cp) {
        String s = java.text.Normalizer.normalize(new String(Character.toChars(cp)), java.text.Normalizer.Form.NFD);
        return s.isEmpty() ? cp : s.codePointAt(0);
    }

    private static int[] rows(String hex) {
        int[] out = new int[HEIGHT];
        for (int i = 0; i < HEIGHT; i++) out[i] = Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        return out;
    }
}
