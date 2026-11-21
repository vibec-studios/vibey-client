package dev.henny.vibeyclient.client.hud;

import dev.henny.vibeyclient.ui.Gfx;

import java.util.EnumMap;
import java.util.Map;

public final class VanillaLayers {
    private VanillaLayers() {}

    public enum Layer { EFFECTS, BOSS_BAR, TITLE, SCOREBOARD, PLAYER_LIST }

    public interface Replacement {
        boolean draw(Gfx g, Runnable vanilla);
    }

    private static final Map<Layer, Replacement> REPLACEMENTS = new EnumMap<>(Layer.class);
    private static final boolean[] INSIDE = new boolean[Layer.values().length];

    public static synchronized void replace(Layer layer, Replacement replacement) {
        REPLACEMENTS.put(layer, replacement);
    }

    public static boolean render(Layer layer, Object graphics, Runnable vanilla) {
        int i = layer.ordinal();
        if (INSIDE[i]) return false;
        Replacement r = REPLACEMENTS.get(layer);
        if (r == null) return false;
        INSIDE[i] = true;
        try {
            return r.draw(Gfx.of(graphics), vanilla);
        } catch (RuntimeException e) {
            return false;
        } finally {
            INSIDE[i] = false;
        }
    }
}
