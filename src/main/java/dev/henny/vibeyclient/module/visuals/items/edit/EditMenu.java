package dev.henny.vibeyclient.module.visuals.items.edit;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.visuals.items.core.EditSession;
import dev.henny.vibeyclient.module.visuals.items.core.HandPreset;

import java.util.ArrayList;
import java.util.List;

public final class EditMenu {
    public record Entry(String id, String icon, String label, boolean on) {}

    public record Menu(float x, float y, List<Entry> items) {}

    static final String RESET = "reset", COPY = "copy";

    private EditMenu() {}

    static Menu build(EditSession session, float x, float y) {
        HandPreset now = session.matchingPreset();
        List<Entry> items = new ArrayList<>();
        for (HandPreset p : HandPreset.values()) {
            items.add(new Entry(p.id(), p == HandPreset.VANILLA ? "cube" : "sliders",
                    Lang.tr("visuals.items.preset." + p.id()), p == now));
        }
        items.add(new Entry(COPY, "copy", Lang.tr("visuals.items.edit.copy"), false));
        items.add(new Entry(RESET, "reset", Lang.tr("visuals.items.edit.reset"), false));
        return new Menu(x, y, items);
    }

    static void pick(EditSession session, String id) {
        switch (id) {
            case RESET -> session.reset();
            case COPY -> session.copyToOther();
            default -> {
                HandPreset p = HandPreset.byId(id);
                if (p != null) session.preset(p);
            }
        }
    }
}
