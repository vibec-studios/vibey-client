package dev.henny.vibeyclient.module.clips.shots.core;

public final class ArmedConfirm {
    public static final long WINDOW_MS = 3000;
    private String key;
    private long armedAt;

    public boolean press(String target, long nowMs) {
        if (target != null && target.equals(key) && nowMs - armedAt <= WINDOW_MS) {
            key = null;
            return true;
        }
        key = target;
        armedAt = nowMs;
        return false;
    }

    public boolean armed(String target, long nowMs) {
        return target != null && target.equals(key) && nowMs - armedAt <= WINDOW_MS;
    }

    public void clear() {
        key = null;
    }
}
