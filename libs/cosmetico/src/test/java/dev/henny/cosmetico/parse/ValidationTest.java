package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.fixtures.Fixtures;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationTest {
    private record Rule(String name, boolean emote, Consumer<JsonObject> change, String message) {}

    private static JsonObject bone(JsonObject o, int i) {
        return o.getAsJsonObject("model").getAsJsonArray("bones").get(i).getAsJsonObject();
    }

    private static void bones(JsonObject o, int n) {
        JsonArray a = new JsonArray();
        for (int i = 0; i < n; i++) a.add(Fixtures.json("{\"name\":\"b" + i + "\",\"attach\":\"body\"}"));
        o.getAsJsonObject("model").add("bones", a);
    }

    private static final List<Rule> RULES = List.of(
            new Rule("format", false, o -> o.addProperty("format", "zip"), "format must be \"vibey\""),
            new Rule("formatVersion", false, o -> o.addProperty("formatVersion", 0), "formatVersion must be 1"),
            new Rule("kind", false, o -> o.addProperty("kind", "hat"), "kind must be cosmetic or emote"),
            new Rule("id pattern", false, o -> o.addProperty("id", "NoColon"), "is not a valid namespace:path id"),
            new Rule("id length", false, o -> o.addProperty("id", "a:" + "x".repeat(127)), "is not a valid namespace:path id"),
            new Rule("name", false, o -> o.remove("name"), "name.en is required"),
            new Rule("slot", false, o -> o.getAsJsonObject("cosmetic").addProperty("slot", "tail"), "cosmetic.slot \"tail\" is unknown"),
            new Rule("model required", false, o -> o.remove("model"), "model is required for slot hat"),
            new Rule("png", false, o -> o.getAsJsonArray("textures").get(0).getAsJsonObject().addProperty("png", "AAAA"), "texture main is not a PNG"),
            new Rule("png width", false, o -> o.getAsJsonArray("textures").get(0).getAsJsonObject().addProperty("png", Fixtures.png(600, 16)), "px wide (max 512)"),
            new Rule("texture id", false, o -> o.getAsJsonArray("textures").add(o.getAsJsonArray("textures").get(0).deepCopy()), "duplicate texture id main"),
            new Rule("parent missing", false, o -> bone(o, 0).addProperty("parent", "x"), "parent \"x\" does not exist"),
            new Rule("parent cycle", false, o -> bone(o, 0).addProperty("parent", "b"), "parent cycle"),
            new Rule("bone name", false, o -> o.getAsJsonObject("model").getAsJsonArray("bones").add(bone(o, 0).deepCopy()), "duplicate bone \"b\""),
            new Rule("bone count", false, o -> bones(o, 257), "257 bones (max 256)"),
            new Rule("prices", false, o -> o.addProperty("price", 5), "\"price\" is not allowed"),
            new Rule("pet scale", false, o -> o.getAsJsonObject("cosmetic").add("pet", Fixtures.json("{\"scale\":9}")), "cosmetic.pet.scale must be a positive number"),
            new Rule("locator", false, o -> o.getAsJsonObject("model").add("locators", Fixtures.json("{\"l\":{\"bone\":\"nope\"}}")), "bone \"nope\" does not exist"),
            new Rule("length", false, o -> Fixtures.animation(o, "idle", "{\"length\":0}"), "length must be > 0"),
            new Rule("sorted keys", false, o -> Fixtures.animation(o, "idle", "{\"length\":1,\"bones\":{\"b\":{\"rotation\":[{\"t\":1,\"v\":[0,0,0]},{\"t\":0,\"v\":[0,0,0]}]}}}"), "keyframes must be sorted by t"),
            new Rule("sound", false, o -> o.add("sounds", Fixtures.json("{\"a\":[{\"id\":\"s\",\"ogg\":\"AAAA\"}]}").getAsJsonArray("a")), "sound s is not OGG"),
            new Rule("emote animation", true, o -> o.getAsJsonObject("emote").addProperty("animation", "dance"), "emote.animation \"dance\" does not exist"),
            new Rule("emote bone name", true, o -> o.add("model", Fixtures.json("{\"bones\":[{\"name\":\"head\"}]}")), "clashes with a player bone name"),
            new Rule("size", false, o -> o.addProperty("padding", "x".repeat(2 * 1024 * 1024)), "bytes (max 2097152)"));

    @TestFactory
    Stream<DynamicTest> everyRuleRejects() {
        return RULES.stream().map(r -> DynamicTest.dynamicTest(r.name(), () -> {
            JsonObject o = r.emote() ? Fixtures.emote("t:e") : Fixtures.cosmetic("t:x", "hat");
            r.change().accept(o);
            BundleException e = assertThrows(BundleException.class, () -> Fixtures.parse(o));
            assertTrue(e.getMessage().contains(r.message()), e.getMessage());
        }));
    }
}
