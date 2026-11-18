package dev.henny.vibeyclient.net;

import io.netty.handler.codec.DecoderException;

import java.util.UUID;
import java.util.regex.Pattern;

/** Shared protocol constants and size limits of the {@code vibeyclient:*} channel (SPEC §6). */
public final class NetLimits {
    private NetLimits() {}

    /** Bumped on incompatible changes; the server answers hello with its own version. */
    public static final int PROTOCOL = 1;

    public static final int MAX_SLOTS = 16;
    public static final int MAX_SLOT_NAME = 32;
    public static final int MAX_ITEM_ID = 128;
    public static final int MAX_CHANNEL = 64;
    public static final int MAX_MESSAGE_BYTES = 8192;

    /** UUID used for "no player" (client to server packets, broadcast target). */
    public static final UUID NOBODY = new UUID(0L, 0L);

    public static final Pattern SLOT = Pattern.compile("[a-z_]{1,32}");
    /** Cosmetic/emote ids as in the .vibey format (SPEC §5), or empty. */
    public static final Pattern ITEM_ID = Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)?");
    public static final Pattern CHANNEL = Pattern.compile("[a-z0-9_.:-]{1,64}");

    static int checkCount(int n, int max) {
        if (n < 0 || n > max) throw new DecoderException("vibeyclient: too many entries (" + n + ")");
        return n;
    }
}
