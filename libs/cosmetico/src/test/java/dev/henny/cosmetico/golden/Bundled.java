package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.parse.Bundles;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Bundled {
    private Bundled() {}

    private static Map<String, Bundle> parsed;

    public static synchronized Map<String, Bundle> parsed() {
        if (parsed != null) return parsed;
        Map<String, Bundle> out = new LinkedHashMap<>();
        for (String f : Golden.bundles()) {
            try {
                out.put(f, Bundles.parse(Golden.read(f), GoldenVersion.gate()));
            } catch (RuntimeException ignored) {
            }
        }
        parsed = out;
        return out;
    }

    public static Map<Slot, Bundle> firstPerSlot() {
        Map<Slot, Bundle> others = new EnumMap<>(Slot.class);
        for (Bundle b : parsed().values()) if (b.slot != null) others.putIfAbsent(b.slot, b);
        return others;
    }
}
