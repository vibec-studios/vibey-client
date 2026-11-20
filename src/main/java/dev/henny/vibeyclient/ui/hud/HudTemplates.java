package dev.henny.vibeyclient.ui.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class HudTemplates {
    private HudTemplates() {}

    public record Template(String id, String labelKey, String descKey, Supplier<HudLayout.State> state) {}

    private static final List<Template> ALL = new ArrayList<>();
    private static String defaultId;

    public static void setDefault(String id) {
        defaultId = id;
    }

    public static String defaultId() {
        return defaultId;
    }

    public static synchronized void register(Template t) {
        ALL.removeIf(x -> x.id().equals(t.id()));
        ALL.add(t);
    }

    public static synchronized List<Template> all() {
        return List.copyOf(ALL);
    }

    public static synchronized Template get(String id) {
        for (Template t : ALL) if (t.id().equals(id)) return t;
        return null;
    }

    public static boolean apply(String id) {
        Template t = get(id);
        if (t == null) return false;
        try {
            HudLayout.apply(t.state().get(), true);
            return true;
        } catch (RuntimeException e) {
            dev.henny.vibeyclient.Vibey.LOG.warn("HUD template {} failed", id, e);
            return false;
        }
    }
}
