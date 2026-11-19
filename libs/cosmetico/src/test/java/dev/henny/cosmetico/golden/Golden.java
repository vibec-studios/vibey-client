package dev.henny.cosmetico.golden;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class Golden {
    private Golden() {}

    public static Path root() {
        Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (p != null && !Files.isDirectory(p.resolve("content/dist"))) p = p.getParent();
        if (p == null) throw new IllegalStateException("content/dist not found above " + System.getProperty("user.dir"));
        return p;
    }

    public static Path dir() {
        return root().resolve("libs/cosmetico/src/test/resources/golden");
    }

    public static List<String> bundles() {
        List<String> out = new ArrayList<>();
        for (String d : new String[]{"dist", "dist/polar-pals", "audit"}) {
            try (Stream<Path> s = Files.list(root().resolve("content").resolve(d))) {
                s.filter(f -> f.toString().endsWith(".vibey")).map(f -> d + "/" + f.getFileName()).sorted().forEach(out::add);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return out;
    }

    public static byte[] read(String bundle) {
        try {
            return Files.readAllBytes(root().resolve("content").resolve(bundle));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String name(String bundle) {
        return bundle.replace("dist/polar-pals/", "polar-pals/").replace(".vibey", "");
    }

    public static boolean exists(String rel) {
        return Files.exists(dir().resolve(rel));
    }

    public static String load(String rel) {
        try {
            return Files.readString(dir().resolve(rel), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
