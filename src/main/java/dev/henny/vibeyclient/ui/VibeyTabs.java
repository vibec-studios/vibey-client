package dev.henny.vibeyclient.ui;

import dev.henny.vibeyclient.ui.screen.ModulesPanel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Top-level tabs of the Vibey menu. Modules add theirs in {@code onLoad}, e.g.
 * {@code VibeyTabs.register(new VibeyTabs.Tab("friends", "social.tab", 10, FriendsPanel::new, () -> isEnabled()))}.
 * Label key is a Vibey i18n key. {@code order}: modules = 0, lower first.
 */
public final class VibeyTabs {
    private VibeyTabs() {}

    public record Tab(String id, String labelKey, int order, Supplier<Panel> factory, java.util.function.BooleanSupplier visible) {
        public Tab(String id, String labelKey, int order, Supplier<Panel> factory) {
            this(id, labelKey, order, factory, () -> true);
        }
    }

    public static final String MODULES = "modules";

    private static final List<Tab> TABS = new ArrayList<>();

    static {
        register(new Tab(MODULES, "core.tab.modules", 0, ModulesPanel::new));
    }

    public static synchronized void register(Tab tab) {
        TABS.removeIf(t -> t.id().equals(tab.id()));
        TABS.add(tab);
        TABS.sort(Comparator.comparingInt(Tab::order));
    }

    public static synchronized List<Tab> visible() {
        List<Tab> out = new ArrayList<>();
        for (Tab t : TABS) if (t.visible().getAsBoolean()) out.add(t);
        return out;
    }
}
