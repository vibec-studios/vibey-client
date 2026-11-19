package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class CoreGoldenTest {
    @TestFactory
    List<DynamicTest> meshesAreByteEqual() {
        List<DynamicTest> out = new ArrayList<>();
        for (Map.Entry<String, Bundle> e : Bundled.parsed().entrySet()) {
            if (e.getValue().model == null) continue;
            String rel = "mesh/" + Golden.name(e.getKey()) + ".txt";
            out.add(DynamicTest.dynamicTest(rel, () -> GoldenCompare.exact(rel, MeshDump.dump(e.getValue()))));
        }
        return out;
    }

    @TestFactory
    List<DynamicTest> boneMatricesMatch() {
        Map<Slot, Bundle> others = Bundled.firstPerSlot();
        List<DynamicTest> out = new ArrayList<>();
        for (Map.Entry<String, Bundle> e : Bundled.parsed().entrySet()) {
            if (e.getValue().model == null) continue;
            String rel = "pose/" + Golden.name(e.getKey()) + ".txt";
            out.add(DynamicTest.dynamicTest(rel, () -> GoldenCompare.close(rel, PoseDump.dump(e.getValue(), others))));
        }
        return out;
    }

    @TestFactory
    List<DynamicTest> animationSamplesMatch() {
        List<DynamicTest> out = new ArrayList<>();
        for (Map.Entry<String, Bundle> e : Bundled.parsed().entrySet()) {
            if (e.getValue().animations.isEmpty()) continue;
            String rel = "anim/" + Golden.name(e.getKey()) + ".txt";
            out.add(DynamicTest.dynamicTest(rel, () -> GoldenCompare.exact(rel, AnimDump.dump(e.getValue()))));
        }
        return out;
    }

    @Test
    void statesMatch() {
        GoldenCompare.exact("states.txt", StatesDump.dump(new ArrayList<>(Bundled.parsed().values())));
    }

    @Test
    void versionsMatch() {
        GoldenCompare.exact("versions.txt", MiscDump.versions());
    }

    @Test
    void registryMatches() {
        GoldenCompare.exact("registry.txt", MiscDump.registry());
    }
}
