package dev.henny.r3.object;

import dev.henny.r3.panel.KeyContent;
import dev.henny.r3.panel.PanelContent;
import dev.henny.r3.panel.PointerButton;
import dev.henny.r3.panel.PointerContent;
import dev.henny.r3.render.TextureHandle;
import dev.henny.r3.text.TextWrap;

public final class VideoSurface implements ObjectContent, PointerContent, KeyContent {
    public static final float DEFAULT_BEZEL = 0.04f;
    public static final int DEFAULT_BEZEL_COLOR = 0xFF0E0E12;
    public static final int DEFAULT_PANEL_COLOR = 0xFF16161C;
    public static final int DEFAULT_STATUS_COLOR = 0xFFD8D8E0;

    private TextureHandle texture;
    private float u0 = 0f;
    private float v0 = 0f;
    private float u1 = 1f;
    private float v1 = 1f;
    private float bezel = DEFAULT_BEZEL;
    private int bezelColor = DEFAULT_BEZEL_COLOR;
    private int panelColor = DEFAULT_PANEL_COLOR;
    private int statusColor = DEFAULT_STATUS_COLOR;
    private final TextWrap statusWrap = new TextWrap();
    private String status;
    private long drawnFrames;
    private VideoOverlay overlay;

    public TextWrap statusWrap() {
        return statusWrap;
    }

    public TextureHandle texture() {
        return texture;
    }

    public VideoSurface texture(TextureHandle texture) {
        this.texture = texture;
        return this;
    }

    public VideoSurface crop(float u0, float v0, float u1, float v1) {
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        return this;
    }

    public float u0() {
        return u0;
    }

    public float v0() {
        return v0;
    }

    public float u1() {
        return u1;
    }

    public float v1() {
        return v1;
    }

    public float aspect() {
        if (texture == null || texture.height() <= 0) return 16f / 9f;
        return texture.width() * Math.abs(u1 - u0) / (texture.height() * Math.abs(v1 - v0));
    }

    public float bezel() {
        return bezel;
    }

    public VideoSurface bezel(float meters, int argb) {
        this.bezel = Math.max(0f, meters);
        this.bezelColor = argb;
        return this;
    }

    public int bezelColor() {
        return bezelColor;
    }

    public int panelColor() {
        return panelColor;
    }

    public VideoSurface panelColor(int argb) {
        this.panelColor = argb;
        return this;
    }

    public int statusColor() {
        return statusColor;
    }

    public String status() {
        return status;
    }

    public VideoSurface status(String line, int argb) {
        this.status = line;
        this.statusColor = argb;
        return this;
    }

    public VideoSurface status(String line) {
        this.status = line;
        return this;
    }

    public boolean showsPicture() {
        return texture != null;
    }

    public long drawnFrames() {
        return drawnFrames;
    }

    public void markDrawn() {
        drawnFrames++;
    }

    @Override
    public boolean translucent() {
        return false;
    }

    public VideoOverlay overlay() {
        return overlay;
    }

    public VideoSurface overlay(PanelContent content) {
        overlay = content == null ? null : new VideoOverlay(content);
        return this;
    }

    public boolean overlayShown() {
        return overlay != null && overlay.shown();
    }

    private PanelContent shownOverlay() {
        return overlayShown() ? overlay.content() : null;
    }

    @Override
    public void tick() {
        if (overlay != null) overlay.content().tick();
    }

    @Override
    public void update(float dt) {
        if (overlay != null) overlay.update(dt);
    }

    @Override
    public String controlAt(float x, float y) {
        PanelContent o = shownOverlay();
        return o == null ? null : o.controlAt(x, y);
    }

    @Override
    public void hoverChanged(String controlId) {
        if (overlay != null) overlay.content().hoverChanged(controlId);
    }

    @Override
    public void pointerMove(float x, float y) {
        if (overlay == null) return;
        overlay.entered();
        overlay.content().pointerMove(x, y);
    }

    @Override
    public void pointerExit() {
        if (overlay == null) return;
        overlay.exited();
        overlay.content().pointerExit();
    }

    @Override
    public boolean pointerDown(float x, float y, PointerButton button) {
        PanelContent o = shownOverlay();
        return o != null && o.pointerDown(x, y, button);
    }

    @Override
    public void pointerUp(float x, float y, PointerButton button) {
        if (overlay != null) overlay.content().pointerUp(x, y, button);
    }

    @Override
    public boolean scroll(float x, float y, double amount) {
        PanelContent o = shownOverlay();
        return o != null && o.scroll(x, y, amount);
    }

    @Override
    public boolean keyFocused() {
        return overlay != null && overlay.keyFocused();
    }

    @Override
    public boolean keyDown(int key, int scancode, int mods, boolean repeat) {
        return keyFocused() && ((KeyContent) overlay.content()).keyDown(key, scancode, mods, repeat);
    }

    @Override
    public void keyUp(int key, int scancode, int mods) {
        if (keyFocused()) ((KeyContent) overlay.content()).keyUp(key, scancode, mods);
    }

    @Override
    public boolean charTyped(int codepoint, int mods) {
        return keyFocused() && ((KeyContent) overlay.content()).charTyped(codepoint, mods);
    }

    @Override
    public void releaseKeyFocus() {
        if (overlay != null && overlay.content() instanceof KeyContent k) k.releaseKeyFocus();
    }
}
