package dev.henny.companio.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.pack.DirFiles;
import dev.henny.companio.pack.Pack;
import dev.henny.companio.pack.Problem;
import dev.henny.companio.support.TestPacks;
import dev.henny.cosmetico.version.VersionGate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinPacksTest {
    private static List<Pack> packs() throws IOException {
        Path root = TestPacks.builtinRoot();
        JsonObject index = JsonParser.parseString(Files.readString(root.resolve("packs.json"))).getAsJsonObject();
        List<Pack> out = new ArrayList<>();
        for (JsonElement e : index.getAsJsonArray("packs")) {
            JsonObject p = e.getAsJsonObject();
            Path dir = root.resolve(p.get("id").getAsString());
            JsonArray files = p.getAsJsonArray("files");
            for (JsonElement f : files) assertTrue(Files.isRegularFile(dir.resolve(f.getAsString())), "listed file missing: " + f);
            out.add(PackReader.read(new DirFiles(dir), VersionGate.fixed("99.0.0"), true));
        }
        return out;
    }

    @Test
    void everyBuiltinPackLoadsWithoutErrors() throws IOException {
        List<Pack> packs = packs();
        assertFalse(packs.isEmpty());
        for (Pack p : packs) {
            List<Problem> errors = p.problems().stream().filter(Problem::error).toList();
            assertTrue(errors.isEmpty(), p.location() + ":\n" + errors);
            assertFalse(p.companions().isEmpty(), p.location());
            assertNotNull(p.icon(), p.id() + " has no pack.png");
            for (String lang : List.of("en_us", "de_de")) assertNotEquals(p.name().resolve(p.lang(), lang), "", lang);
        }
    }

    @Test
    void sealUsesEveryFeature() throws IOException {
        Pack seal = packs().stream().filter(p -> "seal".equals(p.id())).findFirst().orElseThrow();
        CompanionDef c = seal.companion("seal");
        assertTrue(c.behaviors().size() >= 30, "behaviors: " + c.behaviors().size());
        assertEquals(3, c.variants().size());
        assertNotSame(c.model("grey"), c.model("arctic"));
        assertTrue(c.props().keySet().containsAll(List.of("rain_hat", "ball", "scarf", "zzz")));
        assertTrue(c.sounds().get("bark").embedded());
        assertFalse(c.sounds().get("splash").embedded());
        assertEquals(5, c.tricks().size());
        assertTrue(c.lookBone() >= 0);
        for (String e : List.of("pet", "pick_up", "place", "rename", "enter_water", "leave_water", "spawn", "owner_hurt", "day_start")) {
            assertTrue(c.behaviors().stream().anyMatch(b -> e.equals(b.on())), "no behavior on " + e);
        }
        assertTrue(seal.problems().stream().noneMatch(Problem::error));
        assertNotNull(c.model("grey").sounds);
        assertTrue(java.util.Arrays.stream(c.model("grey").sounds).anyMatch(s -> s.id().equals("bark")), "pack sounds are embedded into the model");
    }
}
