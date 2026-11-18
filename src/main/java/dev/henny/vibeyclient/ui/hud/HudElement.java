package dev.henny.vibeyclient.ui.hud;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.ui.Gfx;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A 2D HUD element the player can move, resize and style in the HUD editor ({@link HudEditorScreen}). Two kinds:
 *
 * <ul>
 *   <li><b>Widgets</b> ({@link Builder#widget}): the content is a {@link HudWidget} (often a {@link TextWidget}).
 *       The core draws them on the HUD itself, applies the player's {@link HudStyle} and placement, hides them when the
 *       server disabled their {@link Builder#rules rules key}, and lists them in the editor's widget library.
 *       This is the API for new HUD content (see docs/HUD.md).</li>
 *   <li><b>Plain elements</b> ({@link Builder#render}): the owner keeps drawing them (usually in {@code renderHud})
 *       through {@link HudLayout#draw(Gfx, HudElement)}, which applies the placement; the editor calls the same
 *       renderer with {@code preview = true}. Used by media (lyrics, now playing, video overlay).</li>
 * </ul>
 *
 * <pre>{@code
 * HudLayout.register(HudElement.builder("pvp.sprint")
 *         .label("pvp.hud.sprint")                   // i18n key of the name in the editor
 *         .description("pvp.hud.sprint.desc")        // one line in the widget library
 *         .icon(Icons.WAVE).category(HudElement.Category.COMBAT)
 *         .rules("pvp.togglesprint")                 // ServerRules key: hidden when the server disables it
 *         .defaults(Anchor.BOTTOM_LEFT, 4, -4)
 *         .visibleWhen(() -> pvp.isEnabled() && pvp.toggleSprint.get())
 *         .widget(SprintIndicator::new)              // a HudWidget / TextWidget
 *         .build());
 * }</pre>
 */
public final class HudElement {
    /** Draws a plain element into its own box (0, 0)–(w, h); the layout's translate/scale are already applied. */
    @FunctionalInterface
    public interface Renderer {
        void render(Gfx g, int w, int h, boolean preview);
    }

    /** Groups in the editor's widget library ({@code core.hud.cat.<name>}). */
    public enum Category { INFO, COMBAT, MOVEMENT, WORLD, MEDIA, OTHER }

    private final String id;
    private final String baseId;
    private final int copyIndex;
    private final String labelKey;
    private final String descKey;
    private final String icon;
    private final Category category;
    private final String rulesKey;
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
    private final Supplier<? extends HudWidget> widgetFactory;
    private final HudWidget widget;
    private final int minW;
    private final int minH;
    private final boolean freeSize;
    private final List<HudOption> options;
    private final boolean duplicable;
    private final boolean startHidden;
    private final boolean variants;
    /** A failure of the widget was logged once. */
    boolean brokenLogged;

    private HudElement(Builder b) {
        this.id = b.prefix + b.name;
        this.baseId = id;
        this.copyIndex = 1;
        this.labelKey = b.label != null ? b.label : (b.defaultLabel != null ? b.defaultLabel + b.name : id);
        this.descKey = b.desc;
        this.icon = b.icon;
        this.category = b.category != null ? b.category : (b.widget != null ? Category.INFO : Category.MEDIA);
        this.rulesKey = b.rules;
        this.width = b.width;
        this.height = b.height;
        this.defaultAnchor = b.anchor;
        this.defaultX = b.dx;
        this.defaultY = b.dy;
        this.defaultScale = b.scale;
        this.minScale = b.minScale;
        this.maxScale = b.maxScale;
        this.available = b.available;
        this.widgetFactory = b.widget;
        this.widget = b.widget != null ? b.widget.get() : null;
        if (b.widget == null && b.renderer == null) throw new NullPointerException("renderer or widget");
        this.renderer = b.renderer;
        this.minW = b.minW;
        this.minH = b.minH;
        this.freeSize = b.freeSize;
        this.options = List.copyOf(b.options);
        this.duplicable = b.duplicable && b.widget != null;
        this.startHidden = b.startHidden;
        this.variants = b.variants;
        if (rulesKey != null) ServerRules.supports(rulesKey);
    }

    /** A copy for duplicates ({@code <base>.<n>}), with its own widget instance. */
    private HudElement(HudElement base, int index) {
        this.id = base.baseId + "." + index;
        this.baseId = base.baseId;
        this.copyIndex = index;
        this.labelKey = base.labelKey;
        this.descKey = base.descKey;
        this.icon = base.icon;
        this.category = base.category;
        this.rulesKey = base.rulesKey;
        this.width = base.width;
        this.height = base.height;
        this.defaultAnchor = base.defaultAnchor;
        this.defaultX = base.defaultX;
        this.defaultY = base.defaultY + 16 * (index - 1);
        this.defaultScale = base.defaultScale;
        this.minScale = base.minScale;
        this.maxScale = base.maxScale;
        this.renderer = base.renderer;
        this.available = base.available;
        this.widgetFactory = base.widgetFactory;
        this.widget = base.widgetFactory.get();
        this.minW = base.minW;
        this.minH = base.minH;
        this.freeSize = base.freeSize;
        this.options = base.options;
        this.duplicable = true;
        this.startHidden = false;
        this.variants = base.variants;
    }

    HudElement copy(int index) {
        return new HudElement(this, index);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    /** The id of the original for duplicates (else {@link #id()}). */
    public String baseId() {
        return baseId;
    }

    /** 1 for the original, 2, 3, ... for duplicates. */
    public int copyIndex() {
        return copyIndex;
    }

    public boolean isCopy() {
        return copyIndex > 1;
    }

    public String labelKey() {
        return labelKey;
    }

    /** The name shown in the editor (with "2", "3" for duplicates). */
    public String displayName() {
        String n = Lang.tr(labelKey);
        return copyIndex > 1 ? n + " " + copyIndex : n;
    }

    public String descriptionKey() {
        return descKey;
    }

    public String icon() {
        return icon;
    }

    public Category category() {
        return category;
    }

    /** The {@link ServerRules} key, or null. */
    public String rulesKey() {
        return rulesKey;
    }

    /** The server disabled this element ({@link ServerRules}). */
    public boolean serverDisabled() {
        return rulesKey != null && ServerRules.isDisabled(rulesKey);
    }

    /** Natural (unscaled) width in GUI pixels. */
    public int width() {
        if (widget != null) return widget.naturalWidth();
        return Math.max(4, width.getAsInt());
    }

    public int height() {
        if (widget != null) return widget.naturalHeight();
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

    /** The widget content (null for plain elements). */
    public HudWidget widget() {
        return widget;
    }

    public boolean isWidget() {
        return widget != null;
    }

    /** Has a style (background, colours, ...) the player can change: widgets only. */
    public boolean styleable() {
        return widget != null;
    }

    /** Shift-resizing changes the box size instead of the scale (e.g. the compass bar's width). */
    public boolean freeSize() {
        return freeSize;
    }

    public int minWidth() {
        return minW;
    }

    public int minHeight() {
        return minH;
    }

    public List<HudOption> options() {
        return options;
    }

    public boolean duplicable() {
        return duplicable;
    }

    public boolean startHidden() {
        return startHidden;
    }

    /** Offers compact / detailed. */
    public boolean hasVariants() {
        return variants;
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
        if (renderer != null) renderer.render(g, w, h, preview);
    }

    public static final class Builder {
        private String prefix = "";
        private final String name;
        private String label;
        private String defaultLabel;
        private String desc;
        private String icon;
        private Category category;
        private String rules;
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
        private Supplier<? extends HudWidget> widget;
        private int minW = 8;
        private int minH = 8;
        private boolean freeSize;
        private List<HudOption> options = List.of();
        private boolean duplicable;
        private boolean startHidden;
        private boolean variants;

        private Builder(String name) {
            if (!name.matches("[a-z0-9_.]+")) throw new IllegalArgumentException("Bad HUD element id " + name);
            this.name = name;
        }

        /** Vibey i18n key of the name shown in the editor. */
        public Builder label(String key) {
            this.label = key;
            return this;
        }

        /** Vibey i18n key of one short line shown in the widget library. */
        public Builder description(String key) {
            this.desc = key;
            return this;
        }

        /** {@link dev.henny.vibeyclient.ui.Icons} name for the widget library. */
        public Builder icon(String iconName) {
            this.icon = iconName;
            return this;
        }

        public Builder category(Category c) {
            this.category = c;
            return this;
        }

        /** {@link ServerRules} key (declared with {@code supports} for you): hidden in game while disabled. */
        public Builder rules(String key) {
            this.rules = key;
            return this;
        }

        /** Size of a plain element (widgets report theirs). */
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

        /** A plain element drawn by its owner through {@link HudLayout#draw}. */
        public Builder render(Renderer r) {
            this.renderer = r;
            return this;
        }

        /** A widget drawn by the core (one instance per element; duplicates call the factory again). */
        public Builder widget(Supplier<? extends HudWidget> factory) {
            this.widget = factory;
            return this;
        }

        /** Shift-resize changes the box size (down to minW × minH unscaled px) instead of the scale. */
        public Builder freeSize(int minW, int minH) {
            this.freeSize = true;
            this.minW = Math.max(4, minW);
            this.minH = Math.max(4, minH);
            return this;
        }

        public Builder options(HudOption... opts) {
            this.options = List.of(opts);
            return this;
        }

        /** The player may add copies (e.g. one item counter per item). Widgets only. */
        public Builder duplicable() {
            this.duplicable = true;
            return this;
        }

        /** Not on the HUD until the player adds it from the library (or a template shows it). */
        public Builder startHidden() {
            this.startHidden = true;
            return this;
        }

        /** The widget shows more in the "detailed" variant ({@link HudPaint#detailed()}). */
        public Builder variants() {
            this.variants = true;
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
