package dev.henny.vibeyclient.module.pvp;

import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the custom crosshair (also used by the editor's preview). Everything is made of axis-aligned GUI-pixel
 * rectangles, so it stays crisp at every GUI scale; the outline is the same shape one pixel bigger drawn first, and
 * "invert" uses vanilla's inverting blend (always readable, like the vanilla crosshair). Vanilla decides whether a
 * crosshair is shown at all (F1, third person, spectator, debug crosshair): {@code GuiMixin} only swaps the sprite.
 */
public final class Crosshair {
    private Crosshair() {}

    public enum Shape { VANILLA, CROSS, T, CIRCLE, SQUARE, DOT }

    /** One crosshair look (mutable, reused every frame). */
    public static final class Style {
        public Shape shape = Shape.CROSS;
        public int size = 5, gap = 2, thickness = 1;
        public int color = 0xFFFFFFFF;
        public boolean outline = true, dot, invert;
    }

    public static final Identifier VANILLA_SPRITE = Identifier.withDefaultNamespace("hud/crosshair");
    private static final Style HUD_STYLE = new Style();
    private static final int[] RECTS = new int[4 * 512];
    private static int count;
    private static float spread;
    private static long spreadNanos;

    /**
     * The in-game crosshair (called instead of vanilla's sprite). False = draw the vanilla sprite (custom crosshair
     * off, or disabled by the server).
     */
    static boolean drawHud(GuiGraphics gg) {
        PvpModule m = PvpModule.get();
        if (m == null || !m.crosshairActive()) return false;
        Style s = m.crosshairStyle(HUD_STYLE);
        int cx = centre(gg.guiWidth()), cy = centre(gg.guiHeight());
        int extra = m.chDynamic.on() ? Math.round(dynamicSpread(m)) : 0;
        draw(new Gfx(gg, Minecraft.getInstance().font), s, cx, cy, extra);
        return true;
    }

    /** The centre pixel of vanilla's 15 px crosshair sprite on a screen this wide/high. */
    public static int centre(int size) {
        return (size - 15) / 2 + 7;
    }

    /** Extra gap from moving / being in the air, eased per frame (purely visual). */
    private static float dynamicSpread(PvpModule m) {
        LocalPlayer p = Minecraft.getInstance().player;
        float target = 0;
        if (p != null) {
            Vec3 v = p.getDeltaMovement();
            double h = Math.sqrt(v.x * v.x + v.z * v.z);
            target = (float) Math.min(1, h / 0.28);
            if (!p.onGround() && !p.getAbilities().flying) target = Math.max(target, 0.8f);
            if (p.isCrouching()) target *= 0.4f;
        }
        target *= m.chDynamicAmount.get();
        long now = System.nanoTime();
        float dt = spreadNanos == 0 ? 0 : Math.min(0.25f, (now - spreadNanos) / 1e9f);
        spreadNanos = now;
        spread += (target - spread) * (1 - (float) Math.exp(-dt / 0.06f));
        return spread;
    }

    /** Draws {@code s} centred on GUI pixel ({@code cx}, {@code cy}); {@code extraGap} widens it (dynamic). */
    public static void draw(Gfx g, Style s, int cx, int cy, int extraGap) {
        if (s.shape == Shape.VANILLA) {
            g.g.blitSprite(RenderPipelines.CROSSHAIR, VANILLA_SPRITE, cx - 7, cy - 7, 15, 15);
            return;
        }
        int t = Math.max(1, s.thickness);
        int gap = s.gap + extraGap;
        if (s.outline && !s.invert) {
            build(s, cx, cy, t, gap, 1);
            emit(g, (s.color >>> 24) << 24, false);
        }
        build(s, cx, cy, t, gap, 0);
        emit(g, s.color, s.invert);
    }

    private static void emit(Gfx g, int color, boolean invert) {
        for (int i = 0; i < count; i++) {
            int x = RECTS[i * 4], y = RECTS[i * 4 + 1], w = RECTS[i * 4 + 2], h = RECTS[i * 4 + 3];
            if (w <= 0 || h <= 0) continue;
            if (invert) g.g.fill(RenderPipelines.GUI_INVERT, x, y, x + w, y + h, 0xFFFFFFFF);
            else g.rect(x, y, w, h, color);
        }
    }

    /** Fills {@link #RECTS} with the shape's rectangles, each grown by {@code grow} px on every side. */
    private static void build(Style s, int cx, int cy, int t, int gap, int grow) {
        count = 0;
        int o = t / 2;
        int x0 = cx - o, y0 = cy - o; // top-left of the centre t×t block
        int len = Math.max(1, s.size);
        switch (s.shape) {
            case CROSS, T -> {
                rect(x0 - gap - len, y0, len, t, grow);         // left
                rect(x0 + t + gap, y0, len, t, grow);           // right
                rect(x0, y0 + t + gap, t, len, grow);           // bottom
                if (s.shape == Shape.CROSS) rect(x0, y0 - gap - len, t, len, grow); // top
            }
            case SQUARE -> {
                int r = gap + len;
                int ox = x0 - r, oy = y0 - r, side = t + 2 * r;
                rect(ox, oy, side, t, grow);
                rect(ox, oy + side - t, side, t, grow);
                rect(ox, oy + t, t, side - 2 * t, grow);
                rect(ox + side - t, oy + t, t, side - 2 * t, grow);
            }
            case CIRCLE -> ring(cx, cy, gap + len, t, grow);
            case DOT -> {
                int d = 1 + (len - 1) / 2;
                int od = d / 2;
                rect(cx - od, cy - od, d, d, grow);
            }
            default -> {
            }
        }
        if (s.dot && s.shape != Shape.DOT) rect(x0, y0, t, t, grow);
    }

    private static void rect(int x, int y, int w, int h, int grow) {
        if (count * 4 + 4 > RECTS.length) return;
        RECTS[count * 4] = x - grow;
        RECTS[count * 4 + 1] = y - grow;
        RECTS[count * 4 + 2] = w + grow * 2;
        RECTS[count * 4 + 3] = h + grow * 2;
        count++;
    }

    /** A ring of radius {@code r} (to the middle of the line) as horizontal spans, one or two per pixel row. */
    private static void ring(int cx, int cy, int r, int t, int grow) {
        double outer = r + t / 2.0 + grow, inner = r - t / 2.0 - grow;
        int rows = (int) Math.ceil(outer);
        for (int dy = -rows; dy <= rows; dy++) {
            double yy = dy * dy;
            if (yy > outer * outer) continue;
            int xo = (int) Math.floor(Math.sqrt(outer * outer - yy) + 0.5);
            int xi = inner > 0 && yy < inner * inner ? (int) Math.floor(Math.sqrt(inner * inner - yy) + 0.5) : -1;
            if (xi < 0 || xi >= xo) {
                rect(cx - xo, cy + dy, xo * 2 + 1, 1, 0);
            } else {
                rect(cx - xo, cy + dy, xo - xi, 1, 0);
                rect(cx + xi + 1, cy + dy, xo - xi, 1, 0);
            }
        }
    }
}
