package dev.henny.vibeyclient.ui.hud;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.ui.Gfx;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class HudElement {
    @FunctionalInterface
    public interface Renderer {
        void render(Gfx g, int w, int h, boolean preview);
    }

    public enum Category { INFO, COMBAT, MOVEMENT, WORLD, MEDIA, OTHER }

    private final Builder spec;
    private final String id;
    private final int copyIndex;
    private final HudWidget widget;
    boolean brokenLogged;

    private HudElement(Builder spec, int copyIndex) {
        this.spec = spec;
        this.copyIndex = copyIndex;
        this.id = copyIndex > 1 ? spec.id() + "." + copyIndex : spec.id();
        this.widget = spec.widget != null ? spec.widget.get() : null;
        if (spec.widget == null && spec.renderer == null) throw new NullPointerException("renderer or widget");
        if (copyIndex == 1 && spec.rules != null) ServerRules.supports(spec.rules);
    }

    HudElement copy(int index) {
        return new HudElement(spec, index);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    public String baseId() {
        return spec.id();
    }

    public int copyIndex() {
        return copyIndex;
    }

    public boolean isCopy() {
        return copyIndex > 1;
    }

    public String labelKey() {
        return spec.labelKey();
    }

    public String displayName() {
        String n = Lang.tr(labelKey());
        return copyIndex > 1 ? n + " " + copyIndex : n;
    }

    public String descriptionKey() {
        return spec.desc;
    }

    public String icon() {
        return spec.icon;
    }

    public Category category() {
        return spec.category != null ? spec.category : (spec.widget != null ? Category.INFO : Category.MEDIA);
    }

    public String rulesKey() {
        return spec.rules;
    }

    public boolean serverDisabled() {
        return spec.rules != null && ServerRules.isDisabled(spec.rules);
    }

    public int width() {
        return widget != null ? widget.naturalWidth() : Math.max(4, spec.width.getAsInt());
    }

    public int height() {
        return widget != null ? widget.naturalHeight() : Math.max(4, spec.height.getAsInt());
    }

    public Anchor defaultAnchor() {
        return spec.anchor;
    }

    public int defaultX() {
        return spec.dx;
    }

    public int defaultY() {
        return spec.dy + 16 * (copyIndex - 1);
    }

    public float defaultScale() {
        return spec.scale;
    }

    public float minScale() {
        return spec.minScale;
    }

    public float maxScale() {
        return spec.maxScale;
    }

    public HudWidget widget() {
        return widget;
    }

    public boolean isWidget() {
        return widget != null;
    }

    public boolean styleable() {
        return widget != null;
    }

    public boolean freeSize() {
        return spec.freeSize;
    }

    public int minWidth() {
        return spec.minW;
    }

    public int minHeight() {
        return spec.minH;
    }

    public List<HudOption> options() {
        return spec.options;
    }

    public boolean duplicable() {
        return spec.duplicable && widget != null;
    }

    public boolean startHidden() {
        return spec.startHidden && copyIndex == 1;
    }

    public boolean hasVariants() {
        return spec.variants;
    }

    public boolean available() {
        try {
            return spec.available.getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void render(Gfx g, int w, int h, boolean preview) {
        if (spec.renderer != null) spec.renderer.render(g, w, h, preview);
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

        String id() {
            return prefix + name;
        }

        String labelKey() {
            return label != null ? label : defaultLabel != null ? defaultLabel + name : id();
        }

        public Builder label(String key) {
            this.label = key;
            return this;
        }

        public Builder description(String key) {
            this.desc = key;
            return this;
        }

        public Builder icon(String iconName) {
            this.icon = iconName;
            return this;
        }

        public Builder category(Category c) {
            this.category = c;
            return this;
        }

        public Builder rules(String key) {
            this.rules = key;
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

        public Builder widget(Supplier<? extends HudWidget> factory) {
            this.widget = factory;
            return this;
        }

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

        public Builder duplicable() {
            this.duplicable = true;
            return this;
        }

        public Builder startHidden() {
            this.startHidden = true;
            return this;
        }

        public Builder variants() {
            this.variants = true;
            return this;
        }

        public Builder visibleWhen(BooleanSupplier condition) {
            this.available = condition;
            return this;
        }

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
            return new HudElement(this, 1);
        }
    }
}
