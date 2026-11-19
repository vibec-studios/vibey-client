package dev.henny.cosmetico.parse;

import java.util.regex.Pattern;

public final class Limits {
    private Limits() {}

    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final int MAX_TEXTURE = 512, MAX_STRIP = 4096, MAX_BONES = 256, MAX_CUBES = 2048, MAX_SOUND = 512 * 1024;
    public static final int MAX_ID = 128;
    public static final Pattern ID = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");
    public static final int MAX_OFFSET_RULES = 8;
    public static final float MAX_OFFSET = 64f, MAX_OFFSET_ROTATION = 360f;
    public static final String[] FACE_NAMES = {"north", "east", "south", "west", "up", "down"};
}
