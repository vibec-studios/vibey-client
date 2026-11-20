package dev.henny.vibeyclient.ui.hud;

public abstract class HudWidget {
    protected int width = 40;
    protected int height = 12;

    long nextUpdate;
    boolean dirty = true;
    boolean lastPreview;

    protected int rate() {
        return 4;
    }

    protected abstract void update(HudPaint p);

    protected abstract void paint(HudPaint p);

    protected boolean shown(HudPaint p) {
        return true;
    }

    public boolean wheel(HudElement self, int dir) {
        return false;
    }

    public final void invalidate() {
        dirty = true;
    }

    public final int naturalWidth() {
        return Math.max(4, width);
    }

    public final int naturalHeight() {
        return Math.max(4, height);
    }

    final void maybeUpdate(HudPaint p, long nowMs) {
        if (p.preview != lastPreview) {
            lastPreview = p.preview;
            dirty = true;
        }
        int r = rate();
        if (!dirty && r > 0 && nowMs < nextUpdate) return;
        dirty = false;
        nextUpdate = nowMs + (r > 0 ? 1000L / r : 0);
        update(p);
    }
}
