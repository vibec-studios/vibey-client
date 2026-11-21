package dev.henny.vibeafk.control;

public final class SoundControl {
    public static final int AL_GAIN = 0x100A;

    private volatile Class<?> al10;
    private volatile float gameGain = 1f;
    private volatile boolean muted;
    private volatile String lastError;

    public boolean available() {
        return al10 != null;
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

    public float filter(Class<?> owner, float requested) {
        al10 = owner;
        gameGain = requested;
        return muted ? 0f : requested;
    }

    public void setMuted(boolean m) {
        if (m == muted) return;
        muted = m;
        Class<?> c = al10;
        if (c == null) return;
        try {
            c.getMethod("alListenerf", int.class, float.class).invoke(null, AL_GAIN, gameGain);
        } catch (Throwable t) {
            lastError = t.toString();
        }
    }
}
