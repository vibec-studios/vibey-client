package dev.henny.vibeyclient.avatar.golden;

import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.bundle.BundleParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

class OldEngineGoldenTest {
    private static Map<String, Bundle> parsed() throws Exception {
        Map<String, Bundle> out = new LinkedHashMap<>();
        for (String f : GoldenFiles.bundles()) {
            try {
                out.put(f, BundleParser.parse(GoldenFiles.read(f)));
            } catch (RuntimeException ignored) {
            }
        }
        return out;
    }

    @Test
    void parse() throws Exception {
        for (String f : GoldenFiles.bundles()) GoldenFiles.check("parse/" + GoldenFiles.name(f) + ".txt", ParseDump.dump(GoldenFiles.read(f)));
    }

    @Test
    void meshAndPose() throws Exception {
        Map<String, Bundle> all = parsed();
        Map<Slot, Bundle> others = new EnumMap<>(Slot.class);
        for (Bundle b : all.values()) if (b.slot != null) others.putIfAbsent(b.slot, b);
        for (Map.Entry<String, Bundle> e : all.entrySet()) {
            Bundle b = e.getValue();
            if (b.model == null) continue;
            GoldenFiles.check("mesh/" + GoldenFiles.name(e.getKey()) + ".txt", MeshDump.dump(b));
            GoldenFiles.check("pose/" + GoldenFiles.name(e.getKey()) + ".txt", PoseDump.dump(b, others));
        }
    }

    @Test
    void animations() throws Exception {
        for (Map.Entry<String, Bundle> e : parsed().entrySet()) {
            if (e.getValue().animations.isEmpty()) continue;
            GoldenFiles.check("anim/" + GoldenFiles.name(e.getKey()) + ".txt", AnimDump.dump(e.getValue()));
        }
    }

    @Test
    void states() throws Exception {
        GoldenFiles.check("states.txt", StatesDump.dump(new ArrayList<>(parsed().values())));
    }

    @Test
    void versionsAndRegistry() throws Exception {
        GoldenFiles.check("versions.txt", MiscDump.versions());
        GoldenFiles.check("registry.txt", MiscDump.registry());
    }
}
