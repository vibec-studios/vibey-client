package dev.henny.vibeyclient.access;

public record AccessView(AccessPhase phase, AccessInfo info, boolean checking) {
    public static final AccessView START = new AccessView(AccessPhase.CHECKING, null, true);

    public boolean open() {
        return phase == AccessPhase.OPEN;
    }
}
