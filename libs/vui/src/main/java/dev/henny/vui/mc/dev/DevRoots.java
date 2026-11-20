package dev.henny.vui.mc.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class DevRoots {
    public static final String PROPERTY = "vui.dev.roots";
    private static final String MARKER = "libs/vui/src/main/resources";
    private static final int SEARCH_DEPTH = 8;

    private DevRoots() {
    }

    public static List<Path> resolve() {
        String configured = System.getProperty(PROPERTY);
        if (configured != null && !configured.isBlank()) return parse(configured);
        Path repo = findRepository(Path.of("").toAbsolutePath());
        return repo == null ? List.of() : fromRepository(repo);
    }

    static List<Path> parse(String list) {
        List<Path> out = new ArrayList<>();
        for (String part : list.split(";")) {
            String p = part.trim();
            if (!p.isEmpty()) out.add(Path.of(p).toAbsolutePath().normalize());
        }
        return out;
    }

    static Path findRepository(Path start) {
        Path p = start;
        for (int i = 0; i < SEARCH_DEPTH && p != null; i++, p = p.getParent()) {
            if (Files.isDirectory(p.resolve(MARKER))) return p;
        }
        return null;
    }

    static List<Path> fromRepository(Path repo) {
        List<Path> out = new ArrayList<>();
        Path own = repo.resolve("src/main/resources");
        if (Files.isDirectory(own)) out.add(own);
        try (Stream<Path> libs = Files.list(repo.resolve("libs"))) {
            libs.map(lib -> lib.resolve("src/main/resources")).filter(Files::isDirectory).sorted().forEach(out::add);
        } catch (IOException ignored) {
        }
        return out;
    }
}
