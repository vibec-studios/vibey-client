package dev.henny.vibeyclient.module.social.discord.core;

public final class IpcOp {
    public static final int HANDSHAKE = 0;
    public static final int FRAME = 1;
    public static final int CLOSE = 2;
    public static final int PING = 3;
    public static final int PONG = 4;

    private IpcOp() {}
}
