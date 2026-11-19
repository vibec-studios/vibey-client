package dev.henny.vibeyclient.ui.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Ready-made layouts offered in the HUD editor ("PvP", "Minimal", ...). A template is a complete widget layout:
 * applying it hides every widget it does not mention (plain elements such as the lyrics keep their place). The HUD
 * module registers the built-in ones; build states with {@link HudLayout#state()}.
 */
public final class HudTemplates {
    private HudTemplates() {}

    /**
     * @param id       stable id
     * @param labelKey i18n key of the name
     * @param descKey  i18n key of one short line
     * @param state    builds the layout (called when applied, so ids registered later are known)
     */
    public record Template(String id, String labelKey, String descKey, Supplier<HudLayout.State> state) {}

    private static final List<Template> ALL = new ArrayList<>();

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

    /** Applies a template (no undo entry; the editor records one). Returns false if unknown or broken. */
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
