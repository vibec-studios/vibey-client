package dev.henny.vibeyclient.ui.hud;

import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;

import java.util.function.BooleanSupplier;

final class HudCaches {
    private HudCaches() {}

    private static int styleVersion;
    private static int rulesVersion;
    private static BooleanSupplier hideWhen = () -> false;

    static {
        ServerRules.listen(() -> rulesVersion++);
        Events.subscribe(VibeyEvents.LanguageChanged.class, e -> invalidateWidgets());
    }

    static void bumpStyle() {
        styleVersion++;
    }

    static void styleChanged() {
        styleVersion++;
        invalidateWidgets();
    }

    static void invalidateWidgets() {
        for (HudElement e : HudRegistry.widgets) e.widget().invalidate();
    }

    static HudStyle.Resolved resolved(HudLayout.Placement p) {
        if (p.resolvedVersion != styleVersion) {
            HudStyle.resolve(HudRegistry.GLOBAL, p.style, p.resolved);
            p.resolvedVersion = styleVersion;
        }
        return p.resolved;
    }

    static boolean serverDisabled(HudElement e, HudLayout.Placement p) {
        if (e.rulesKey() == null) return false;
        if (p.rulesVersion != rulesVersion) {
            p.rulesDisabled = e.serverDisabled();
            p.rulesVersion = rulesVersion;
        }
        return p.rulesDisabled;
    }

    static void hideWhen(BooleanSupplier condition) {
        hideWhen = condition == null ? () -> false : condition;
    }

    static boolean hiddenNow() {
        try {
            return hideWhen.getAsBoolean();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
