package dev.henny.cosmetico.fixtures;

import java.util.Base64;

public final class TestPng {
    private TestPng() {}

    public static byte[] of(int w, int h) {
        return Base64.getDecoder().decode(Fixtures.png(w, h));
    }
}
