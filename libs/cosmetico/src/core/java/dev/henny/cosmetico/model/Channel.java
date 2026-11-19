package dev.henny.cosmetico.model;

public final class Channel {
    public final float[] t;
    public final float[] v;
    public final float[] pre;
    public final Interp[] interp;

    public Channel(float[] t, float[] v, float[] pre, Interp[] interp) {
        this.t = t;
        this.v = v;
        this.pre = pre;
        this.interp = interp;
    }

    public int size() {
        return t.length;
    }
}
