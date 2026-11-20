package dev.henny.r3.object;

import dev.henny.r3.panel.KeyContent;
import dev.henny.r3.panel.PanelContent;

public final class VideoOverlay {
    public static final float DEFAULT_LINGER = 1.5f;

    private final PanelContent content;
    private float linger = DEFAULT_LINGER;
    private boolean pointerInside;
    private boolean pinned;
    private float sinceExit = Float.MAX_VALUE;

    public VideoOverlay(PanelContent content) {
        this.content = content;
    }

    public PanelContent content() {
        return content;
    }

    public VideoOverlay linger(float seconds) {
        linger = Math.max(0f, seconds);
        return this;
    }

    public boolean pinned() {
        return pinned;
    }

    public VideoOverlay pinned(boolean on) {
        pinned = on;
        return this;
    }

    public boolean shown() {
        return pinned || pointerInside || sinceExit < linger || keyFocused();
    }

    public boolean keyFocused() {
        return content instanceof KeyContent k && k.keyFocused();
    }

    void entered() {
        pointerInside = true;
    }

    void exited() {
        if (pointerInside) sinceExit = 0f;
        pointerInside = false;
    }

    void update(float dt) {
        if (!pointerInside && sinceExit < Float.MAX_VALUE / 2) sinceExit += dt;
        content.update(dt);
    }
}
