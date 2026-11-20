package dev.henny.companio.pack;

public final class PackLimits {
    private PackLimits() {}

    public static final String MANIFEST = "companion.json";
    public static final String FORMAT = "companio-pack";
    public static final int FORMAT_VERSION = 1;
    public static final long MAX_PACK_BYTES = 32L * 1024 * 1024;
    public static final int MAX_FILE_BYTES = 8 * 1024 * 1024;
    public static final int MAX_FILES = 512;
    public static final int MAX_COMPANIONS = 16;
    public static final int MAX_SOUND_BYTES = 512 * 1024;
    public static final int MAX_SOUNDS = 32;
    public static final int MAX_BEHAVIORS = 64;
    public static final int MAX_ACTIONS = 32;
    public static final int MAX_REPEATS = 4;
    public static final float MAX_AT = 30f;
    public static final int MAX_PROPS = 16;
    public static final int MAX_NEEDS = 8;
    public static final int MAX_VARS = 16;
    public static final int MAX_TRICKS = 8;
    public static final int MAX_LAYERS = 4;
    public static final int MAX_PARTICLE_COUNT = 32;
    public static final int MAX_TEXTURE = 512;
    public static final String PACK_ID = "[a-z0-9_.-]{1,48}";
    public static final String ID = "[a-z0-9_]{1,32}";
    public static final String NEED_ID = "[a-z_]{1,24}";
}
