package dev.henny.r3.text;

import dev.henny.r3.anchor.Facing;
import dev.henny.r3.object.ObjectContent;
import dev.henny.r3.object.Transform;
import dev.henny.r3.render.TextMetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Text3D implements ObjectContent {
    public static final float DEFAULT_PIXEL_SIZE = 0.025f;

    private final GlyphLayout layout = new GlyphLayout();
    private final List<GlyphEffect> effects = new ArrayList<>();
    private final List<GlyphEffect> effectsView = Collections.unmodifiableList(effects);
    private String text;
    private TextStyle style = TextStyle.DEFAULT;
    private Facing facing = Facing.CAMERA;
    private float pixelSize = DEFAULT_PIXEL_SIZE;
    private float screenScale;
    private double inFrontDistance;
    private float time;
    private TextMetrics laidOutWith;
    private boolean dirty = true;

    public Text3D(String text) {
        this.text = text == null ? "" : text;
    }

    public String text() {
        return text;
    }

    public Text3D text(String text) {
        String next = text == null ? "" : text;
        if (!next.equals(this.text)) {
            this.text = next;
            dirty = true;
        }
        return this;
    }

    public TextStyle style() {
        return style;
    }

    public Text3D style(TextStyle style) {
        if (style.lineSpacing() != this.style.lineSpacing()) dirty = true;
        this.style = style;
        return this;
    }

    @Override
    public Facing facing() {
        return facing;
    }

    public Text3D facing(Facing facing) {
        this.facing = facing;
        return this;
    }

    public float pixelSize() {
        return pixelSize;
    }

    public Text3D pixelSize(float metersPerFontPixel) {
        this.pixelSize = metersPerFontPixel;
        return this;
    }

    public float screenScale() {
        return screenScale;
    }

    public Text3D constantScreenSize(float factorPerBlock) {
        this.screenScale = Math.max(0f, factorPerBlock);
        return this;
    }

    public double inFrontDistance() {
        return inFrontDistance;
    }

    public Text3D drawInFront(double distance) {
        this.inFrontDistance = Math.max(0, distance);
        return this;
    }

    public List<GlyphEffect> effects() {
        return effectsView;
    }

    public Text3D effect(GlyphEffect effect) {
        effects.add(effect);
        return this;
    }

    public Text3D clearEffects() {
        effects.clear();
        return this;
    }

    public float time() {
        return time;
    }

    public Text3D time(float seconds) {
        time = seconds;
        return this;
    }

    @Override
    public void update(float dt) {
        time += dt;
    }

    public GlyphLayout layout(TextMetrics metrics) {
        if (dirty || laidOutWith != metrics) {
            layout.layout(text, metrics, style.lineSpacing());
            laidOutWith = metrics;
            dirty = false;
        }
        return layout;
    }

    public float worldScale(Transform transform, double distance) {
        float s = pixelSize * transform.scale();
        return screenScale > 0f ? s * (float) (distance * screenScale) : s;
    }

    @Override
    public double boundingRadius(Transform transform) {
        if (laidOutWith == null) return Math.max(1.0, transform.boundingRadius());
        double hw = layout.width() / 2.0 * pixelSize * transform.scale();
        double hh = layout.height() / 2.0 * pixelSize * transform.scale();
        return Math.sqrt(hw * hw + hh * hh) + 0.1;
    }
}
