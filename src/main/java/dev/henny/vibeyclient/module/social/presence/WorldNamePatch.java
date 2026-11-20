package dev.henny.vibeyclient.module.social.presence;

public final class WorldNamePatch {
    public static final long GRACE_MS = 20_000;

    private WorldNamePatch() {}

    public static String name(String world, String sessionName, String presenceWorld, long waitedMs) {
        String n = ServerFields.name(world);
        if (n == null || n.equals(sessionName)) return null;
        boolean adopting = (sessionName == null || sessionName.isBlank()) && n.equals(presenceWorld);
        return adopting && waitedMs < GRACE_MS ? null : n;
    }
}
