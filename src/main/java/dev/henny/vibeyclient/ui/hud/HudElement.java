package dev.henny.vibeyclient.ui.hud;

import dev.henny.vibeyclient.ui.Gfx;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * A 2D HUD element the player can move and scale in the HUD editor ({@link HudEditorScreen}). The owner keeps
 * drawing it (usually in {@code renderHud}) through {@link HudLayout#draw(Gfx, HudElement)}, which applies the
 * player's placement; the editor calls the same renderer with {@code preview = true} so it can show sample content
 * while nothing plays.
 *
 * <pre>{@code
 * HudElement bar = HudLayout.register(HudElement.builder("media.nowplaying")
 *         .label("media.hud.nowplaying")            // Vibey i18n key
 *         .size(260, 17)                             // unscaled size in GUI px (may be suppliers)
 *         .defaults(Anchor.TOP, 0, 4)                // where it sits until the player moves it
 *         .render((g, w, h, preview) -> ...)         // draws into (0, 0)–(w, h); scale/position are applied
 *         .build());
 * }</pre>
 */
public final class HudElement {
    /** Draws the element into its own box (0, 0)–(w, h); the layout's translate/scale are already applied. */
    @FunctionalInterface
    public interface Renderer {
        void render(Gfx g, int w, int h, boolean preview);
    }

    private final String id;
    private final String labelKey;
    private final IntSupplier width;
    private final IntSupplier height;
    private final Anchor defaultAnchor;
    private final int defaultX;
    private final int defaultY;
    private final float defaultScale;
    private final float minScale;
    private final float maxScale;
    private final Renderer renderer;
    private final BooleanSupplier available;

    private HudElement(Builder b) {
        this.id = b.prefix + b.name;
        this.labelKey = b.label != null ? b.label : (b.defaultLabel != null ? b.defaultLabel + b.name : id);
        this.width = b.width;
        this.height = b.height;
        this.defaultAnchor = b.anchor;
        this.defaultX = b.dx;
        this.defaultY = b.dy;
        this.defaultScale = b.scale;
        this.minScale = b.minScale;
        this.maxScale = b.maxScale;
        this.renderer = Objects.requireNonNull(b.renderer, "renderer");
        this.available = b.available;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    public String labelKey() {
        return labelKey;
    }

    /** Unscaled width in GUI pixels. */
    public int width() {
        return Math.max(4, width.getAsInt());
    }

    public int height() {
        return Math.max(4, height.getAsInt());
    }

    public Anchor defaultAnchor() {
        return defaultAnchor;
    }

    public int defaultX() {
        return defaultX;
    }

    public int defaultY() {
        return defaultY;
    }

    public float defaultScale() {
        return defaultScale;
    }

    public float minScale() {
        return minScale;
    }

    public float maxScale() {
        return maxScale;
    }

    /** Shown in the editor at all (e.g. its module is enabled). */
    public boolean available() {
        try {
            return available.getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void render(Gfx g, int w, int h, boolean preview) {
        renderer.render(g, w, h, preview);
    }

    public static final class Builder {
        private String prefix = "";
        private final String name;
        private String label;
        private String defaultLabel;
        private IntSupplier width = () -> 100;
        private IntSupplier height = () -> 20;
        private Anchor anchor = Anchor.TOP_LEFT;
        private int dx;
        private int dy;
        private float scale = 1f;
        private float minScale = 0.5f;
        private float maxScale = 3f;
        private Renderer renderer;
        private BooleanSupplier available = () -> true;

        private Builder(String name) {
            if (!name.matches("[a-z0-9_.]+")) throw new IllegalArgumentException("Bad HUD element id " + name);
            this.name = name;
        }

        /** Vibey i18n key of the name shown in the editor. */
        public Builder label(String key) {
            this.label = key;
            return this;
        }

        public Builder size(int w, int h) {
            return size(() -> w, () -> h);
        }

        public Builder size(IntSupplier w, IntSupplier h) {
            this.width = w;
            this.height = h;
            return this;
        }

        /** Default placement: the element's {@code anchor} point sits on the screen's same point, moved by (dx, dy). */
        public Builder defaults(Anchor anchor, int dx, int dy) {
            this.anchor = anchor;
            this.dx = dx;
            this.dy = dy;
            return this;
        }

        public Builder scale(float def, float min, float max) {
            this.scale = def;
            this.minScale = min;
            this.maxScale = max;
            return this;
        }

        public Builder render(Renderer r) {
            this.renderer = r;
            return this;
        }

        /** Shown in the editor only while this holds (checked every frame, keep it cheap). */
        public Builder visibleWhen(BooleanSupplier condition) {
            this.available = condition;
            return this;
        }

        // used by owners that namespace their elements (e.g. media features)
        public Builder prefix(String p) {
            this.prefix = p;
            return this;
        }

        public Builder defaultLabel(String labelPrefix) {
            this.defaultLabel = labelPrefix;
            return this;
        }

        public Builder andVisible(BooleanSupplier also) {
            BooleanSupplier before = available;
            this.available = () -> also.getAsBoolean() && before.getAsBoolean();
            return this;
        }

        public HudElement build() {
            return new HudElement(this);
        }
    }
}
