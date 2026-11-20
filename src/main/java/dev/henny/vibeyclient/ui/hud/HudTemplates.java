package dev.henny.vibeyclient.ui.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class HudTemplates {
    private HudTemplates() {}

    public record Template(String id, String labelKey, String descKey, Supplier<HudLayout.State> state, String profile) {
        public Template(String id, String labelKey, String descKey, Supplier<HudLayout.State> state) {
            this(id, labelKey, descKey, state, HudProfile.MAIN);
        }
    }

    private static final List<Template> ALL = new ArrayList<>();
    private static final Map<String, String> DEFAULTS = new HashMap<>();

    public static void setDefault(String id) {
        setDefault(HudProfile.MAIN, id);
    }

    public static synchronized void setDefault(String profile, String id) {
        DEFAULTS.put(profile, id);
    }

    public static synchronized String defaultId() {
        return DEFAULTS.get(HudProfiles.active().id());
    }

    public static synchronized void register(Template t) {
        ALL.removeIf(x -> x.id().equals(t.id()));
        ALL.add(t);
    }

    public static synchronized List<Template> all() {
        String p = HudProfiles.active().id();
        List<Template> out = new ArrayList<>();
        for (Template t : ALL) if (t.profile().equals(p)) out.add(t);
        return List.copyOf(out);
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
