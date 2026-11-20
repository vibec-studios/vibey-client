package dev.henny.vibeyclient.module.screens.core;

import java.util.LinkedHashMap;
import java.util.Map;

public record ScreenLayout(float scale, int dim, int blur, boolean hideTitle, Map<String, Placement> widgets) {
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 2f;
    public static final int VANILLA = -1;
    public static final int MAX_DIM = 100;
    public static final int MAX_BLUR = 10;
    public static final ScreenLayout EMPTY = new ScreenLayout(1f, VANILLA, VANILLA, false, Map.of());

    public ScreenLayout {
        scale = Float.isFinite(scale) ? Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale)) : 1f;
        dim = dim < 0 ? VANILLA : Math.min(MAX_DIM, dim);
        blur = blur < 0 ? VANILLA : Math.min(MAX_BLUR, blur);
        Map<String, Placement> copy = new LinkedHashMap<>();
        if (widgets != null) widgets.forEach((k, v) -> {
            if (k != null && v != null && !v.isDefault()) copy.put(k, v);
        });
        widgets = Map.copyOf(copy);
    }

    public boolean isEmpty() {
        return scale == 1f && dim == VANILLA && blur == VANILLA && !hideTitle && widgets.isEmpty();
    }

    public Placement placement(String id) {
        Placement p = widgets.get(id);
        return p == null ? Placement.DEFAULT : p;
    }

    public ScreenLayout with(String id, Placement p) {
        Map<String, Placement> m = new LinkedHashMap<>(widgets);
        if (p == null || p.isDefault()) m.remove(id);
        else m.put(id, p);
        return new ScreenLayout(scale, dim, blur, hideTitle, m);
    }

    public ScreenLayout withScale(float s) {
        return new ScreenLayout(s, dim, blur, hideTitle, widgets);
    }

    public ScreenLayout withDim(int d) {
        return new ScreenLayout(scale, d, blur, hideTitle, widgets);
    }

    public ScreenLayout withBlur(int b) {
        return new ScreenLayout(scale, dim, b, hideTitle, widgets);
    }

    public ScreenLayout withHideTitle(boolean h) {
        return new ScreenLayout(scale, dim, blur, h, widgets);
    }
}
