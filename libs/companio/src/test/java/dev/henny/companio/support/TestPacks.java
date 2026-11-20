package dev.henny.companio.support;

import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.pack.MapFiles;
import dev.henny.companio.pack.Pack;
import dev.henny.companio.parse.PackReader;
import dev.henny.cosmetico.version.VersionGate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TestPacks {
    private TestPacks() {}

    public static final String PNG_1x1 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    public static String model() {
        return """
                {"format":"vibey","formatVersion":1,"kind":"cosmetic","id":"test:blob","name":{"en":"Blob","de":"Blob"},
                 "author":"t","tags":[],"rarity":"common","cosmetic":{"slot":"pet","pet":{"follow":"ground","scale":1}},
                 "textures":[{"id":"main","width":1,"height":1,"png":"%s"}],
                 "model":{"texture":"main","bones":[
                   {"name":"body","parent":null,"attach":"world","pivot":[0,0,0],"cubes":[{"from":[-4,0,-4],"to":[4,8,4],"uv":{"box":[0,0]}}]},
                   {"name":"head","parent":"body","pivot":[0,8,0],"cubes":[{"from":[-3,8,-3],"to":[3,14,3],"uv":{"box":[0,0]}}]},
                   {"name":"hat","parent":"head","pivot":[0,14,0],"cubes":[{"from":[-2,14,-2],"to":[2,16,2],"uv":{"box":[0,0]}}]}],
                  "locators":{"top":{"bone":"head","position":[0,15,0]}}},
                 "animations":{
                   "idle":{"length":2,"loop":true,"bones":{"head":{"rotation":[{"t":0,"v":[0,0,0]},{"t":1,"v":[5,0,0]},{"t":2,"v":[0,0,0]}]}}},
                   "walk":{"length":1,"loop":true,"bones":{"body":{"position":[{"t":0,"v":[0,0,0]},{"t":0.5,"v":[0,1,0]},{"t":1,"v":[0,0,0]}]}}},
                   "clap":{"length":0.5,"loop":false,"bones":{"head":{"rotation":[{"t":0,"v":[0,0,0]},{"t":0.5,"v":[0,10,0]}]}}},
                   "spin":{"length":1,"loop":true,"bones":{"body":{"rotation":[{"t":0,"v":[0,0,0]},{"t":1,"v":[0,360,0]}]}}}}}
                """.formatted(PNG_1x1);
    }

    public static Map<String, String> files(String companionJson) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("companion.json", """
                {"format":"companio-pack","formatVersion":1,"id":"test","version":"1","name":"@pack.name","author":"me",
                 "companions":["companions/blob.json"]}""");
        m.put("lang/en_us.json", "{\"pack.name\":\"Test\",\"blob.name\":\"Blob\",\"say.hi\":[\"Hi\",\"Hello\"]}");
        m.put("lang/de_de.json", "{\"pack.name\":\"Test DE\",\"blob.name\":\"Klecks\"}");
        m.put("models/blob.vibey", model());
        m.put("companions/blob.json", companionJson);
        return m;
    }

    public static String companion(String behaviors) {
        return """
                {"id":"blob","name":"@blob.name","model":"models/blob.vibey","scale":1,
                 "size":{"default":1,"min":0.5,"max":2},"box":{"width":0.5,"height":0.9},
                 "props":{"hat":{"bones":["hat"]}},
                 "sounds":{"boop":{"sound":"minecraft:entity.cat.purr","volume":0.5}},
                 "particles":{"hearts":{"particle":"minecraft:heart","count":2,"locator":"top"}},
                 "needs":{"energy":{"start":1,"perMinute":-1}},
                 "vars":{"count":0},
                 %s
                 "behaviors":%s}""".formatted(behaviors.contains("\"spin\"") ? "\"tricks\":[{\"id\":\"spin\",\"name\":\"Spin\",\"behavior\":\"spin\"}]," : "", behaviors);
    }

    public static Pack load(Map<String, String> files) {
        return PackReader.read(MapFiles.ofText("test", files), VersionGate.fixed("99.0.0"), false);
    }

    public static CompanionDef blob(String behaviors) {
        Pack p = load(files(companion(behaviors)));
        if (p.errors() > 0) throw new AssertionError("pack has errors: " + p.problems());
        return p.companions().get(0);
    }

    public static Path builtinRoot() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null) {
            Path c = p.resolve("libs/companio/src/main/resources/assets/companio/companionpacks");
            if (Files.isDirectory(c)) return c;
            Path d = p.resolve("src/main/resources/assets/companio/companionpacks");
            if (Files.isDirectory(d)) return d;
            p = p.getParent();
        }
        throw new AssertionError("built-in packs not found");
    }
}
