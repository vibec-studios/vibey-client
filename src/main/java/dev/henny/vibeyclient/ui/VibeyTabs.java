package dev.henny.vibeyclient.ui;

import dev.henny.vibeyclient.ui.screen.AboutPanel;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Top-level tabs of the Vibey menu. Modules add theirs in {@code onLoad}, e.g.
 * {@code VibeyTabs.register(new VibeyTabs.Tab("friends", "social.tab", 10, FriendsPanel::new, () -> isEnabled(), Icons.FRIENDS))}.
 * Label key is a Vibey i18n key; the optional icon is an {@link Icons} name (used when the window is too narrow for
 * all labels). {@code order}: modules = 0, about = 1000, lower first. Conventional ids: "wardrobe", "friends",
 * "account" (the title screen and pause entries open these when they exist).
 */
public final class VibeyTabs {
    private VibeyTabs() {}

    public record Tab(String id, String labelKey, int order, Supplier<Panel> factory, BooleanSupplier visible, String icon) {
        public Tab(String id, String labelKey, int order, Supplier<Panel> factory, BooleanSupplier visible) {
            this(id, labelKey, order, factory, visible, null);
        }

        public Tab(String id, String labelKey, int order, Supplier<Panel> factory) {
            this(id, labelKey, order, factory, () -> true, null);
        }
    }

    public static final String MODULES = "modules";
    public static final String ABOUT = "about";

    private static final List<Tab> TABS = new ArrayList<>();

    static {
        register(new Tab(MODULES, "core.tab.modules", 0, ModulesPanel::new, () -> true, Icons.MODULES));
        register(new Tab(ABOUT, "core.tab.about", 1000, AboutPanel::new, () -> true, Icons.INFO));
    }

    public static synchronized void register(Tab tab) {
        TABS.removeIf(t -> t.id().equals(tab.id()));
        TABS.add(tab);
        TABS.sort(Comparator.comparingInt(Tab::order));
    }

    public static synchronized List<Tab> visible() {
        List<Tab> out = new ArrayList<>();
        for (Tab t : TABS) {
            boolean v;
            try {
                v = t.visible().getAsBoolean();
            } catch (RuntimeException e) {
                v = false;
            }
            if (v) out.add(t);
        }
        return out;
    }

    /** True if a visible tab with this id exists. */
    public static boolean exists(String id) {
        for (Tab t : visible()) if (t.id().equals(id)) return true;
        return false;
    }
}
