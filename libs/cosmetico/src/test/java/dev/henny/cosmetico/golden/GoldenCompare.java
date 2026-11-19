package dev.henny.cosmetico.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public final class GoldenCompare {
    private GoldenCompare() {}

    public static final double EPS = 1e-5;

    public static void exact(String rel, String actual) {
        assertEquals(Golden.load(rel), actual, "golden " + rel);
    }

    public static void close(String rel, String actual) {
        String[] want = Golden.load(rel).split("\n", -1), got = actual.split("\n", -1);
        if (want.length != got.length) fail("golden " + rel + ": " + got.length + " lines, expected " + want.length);
        for (int i = 0; i < want.length; i++) {
            String why = LineCompare.diff(want[i], got[i], EPS);
            if (why != null) fail("golden " + rel + " line " + (i + 1) + ": " + why + "\n  want " + want[i] + "\n  got  " + got[i]);
        }
    }
}
