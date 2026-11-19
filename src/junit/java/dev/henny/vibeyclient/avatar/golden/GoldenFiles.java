package dev.henny.vibeyclient.avatar.golden;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GoldenFiles {
    private GoldenFiles() {}

    static Path root() {
        Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (p != null && !Files.isDirectory(p.resolve("content/dist"))) p = p.getParent();
        if (p == null) throw new IllegalStateException("content/dist not found above " + System.getProperty("user.dir"));
        return p;
    }

    static Path golden() {
        return root().resolve("libs/cosmetico/src/test/resources/golden");
    }

    static List<String> bundles() throws IOException {
        List<String> out = new ArrayList<>();
        for (String dir : new String[]{"dist", "dist/polar-pals", "audit"}) {
            try (Stream<Path> s = Files.list(root().resolve("content").resolve(dir))) {
                s.filter(f -> f.toString().endsWith(".vibey")).map(f -> dir + "/" + f.getFileName()).sorted().forEach(out::add);
            }
        }
        return out;
    }

    static byte[] read(String bundle) throws IOException {
        return Files.readAllBytes(root().resolve("content").resolve(bundle));
    }

    static String name(String bundle) {
        return bundle.replace("dist/polar-pals/", "polar-pals/").replace(".vibey", "");
    }

    static void check(String rel, String text) throws IOException {
        Path f = golden().resolve(rel);
        if (!Files.exists(f) || Boolean.getBoolean("cosmetico.golden.write")) {
            Files.createDirectories(f.getParent());
            Files.writeString(f, text, StandardCharsets.UTF_8);
            return;
        }
        String old = Files.readString(f, StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertEquals(old, text, "golden " + rel + " changed");
    }
}
