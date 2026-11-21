package dev.henny.vibeafk.control;

public final class SoundControl {
    public static final int AL_GAIN = 0x100A;
    public static final long REAPPLY_MS = 5000;
    private static final String AL10 = "org.lwjgl.openal.AL10";

    private volatile Class<?> al10;
    private volatile boolean works;
    private volatile boolean ready;
    private volatile ClassLoader alLoader;
    private volatile float gameGain = 1f;
    private volatile boolean muted;
    private volatile String lastError;
    private long lastApply;

    public boolean available() {
        return works;
    }

    public boolean muted() {
        return muted;
    }

    public float gameGain() {
        return gameGain;
    }

    public String lastError() {
        return lastError;
    }

    public void onReady(Class<?> owner) {
        if (owner != null) alLoader = owner.getClassLoader();
        ready = true;
    }

    public boolean ready() {
        return ready;
    }

    public float filter(Class<?> owner, float requested) {
        al10 = owner;
        ready = true;
        works = true;
        gameGain = requested;
        return muted ? 0f : requested;
    }

    public void setMuted(boolean m, ClassLoader loader) {
        if (m == muted) return;
        muted = m;
        apply(loader, System.currentTimeMillis());
    }

    public void tick(ClassLoader loader, long now) {
        if (muted && now - lastApply >= REAPPLY_MS) apply(loader, now);
    }

    private void apply(ClassLoader loader, long now) {
        lastApply = now;
        if (!ready) return;
        Class<?> c = al10;
        ClassLoader l = alLoader != null ? alLoader : loader;
        if (c == null && l != null) {
            try {
                c = Class.forName(AL10, false, l);
            } catch (Throwable t) {
                return;
            }
        }
        if (c == null) return;
        try {
            c.getMethod("alListenerf", int.class, float.class).invoke(null, AL_GAIN, gameGain);
            al10 = c;
            works = true;
        } catch (Throwable t) {
            lastError = t.toString();
        }
    }
}
