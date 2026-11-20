package dev.henny.vibeyclient.module.social.presence;

public final class ServerKind {
    public static final String REMOTE = "remote", LAN = "lan", LOCAL = "local";

    private ServerKind() {}

    public static String of(boolean ownWorld, boolean opened, String serverIp) {
        if (ownWorld) return opened ? LOCAL : null;
        if (serverIp == null || serverIp.isBlank()) return null;
        HostPort hp = HostPort.parse(serverIp);
        return hp == null || LocalAddress.isLocal(hp.host()) ? LAN : REMOTE;
    }

    public static boolean lan(String kind) {
        return LAN.equals(kind) || LOCAL.equals(kind);
    }
}
