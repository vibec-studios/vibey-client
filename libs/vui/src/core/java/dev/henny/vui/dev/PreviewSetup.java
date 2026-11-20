package dev.henny.vui.dev;

import dev.henny.vui.Vui;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.expr.MapTranslator;
import dev.henny.vui.headless.Headless;
import dev.henny.vui.reload.DirectoryRoot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

final class PreviewSetup {
    private PreviewSetup() {
    }

    static Diagnostics apply(PreviewOptions o, Headless headless, Vui vui) throws IOException {
        Diagnostics d = new Diagnostics();
        if (!o.lang().isEmpty()) {
            MapTranslator translator = new MapTranslator();
            for (Path p : o.lang()) LangFiles.load(p, translator, d);
            vui.functions().translator(translator);
        }
        if (o.theme() != null) theme(o.theme(), vui, d);
        for (Path p : o.icons()) icons(p, vui, d);
        for (Path p : o.textures()) headless.textures().addRoot(p.toAbsolutePath().normalize());
        return d;
    }

    private static void theme(Path file, Vui vui, Diagnostics d) {
        Path abs = file.toAbsolutePath().normalize();
        if (!Files.isRegularFile(abs)) {
            d.error(new SourcePos(file.toString(), 1, 1), "theme file not found", null);
            return;
        }
        vui.registry().addRoot(new DirectoryRoot(abs.getParent()));
        vui.loadTheme(abs.getFileName().toString());
        d.addAll(vui.themeDiagnostics());
    }

    private static void icons(Path path, Vui vui, Diagnostics d) throws IOException {
        if (Files.isDirectory(path)) {
            List<Path> files;
            try (Stream<Path> list = Files.list(path)) {
                files = list.filter(p -> p.toString().endsWith(".json")).sorted().toList();
            }
            for (Path f : files) icons(f, vui, d);
            return;
        }
        if (!Files.isRegularFile(path)) {
            d.error(new SourcePos(path.toString(), 1, 1), "icons file not found", null);
            return;
        }
        vui.icons().load(namespace(path), path.toString(), Files.readString(path, StandardCharsets.UTF_8));
        d.addAll(vui.icons().diagnostics());
    }

    static String namespace(Path file) {
        String norm = file.toAbsolutePath().normalize().toString().replace('\\', '/');
        int assets = norm.lastIndexOf("/assets/");
        if (assets >= 0) {
            String rest = norm.substring(assets + 8);
            int slash = rest.indexOf('/');
            if (slash > 0) return rest.substring(0, slash);
        }
        String name = file.getFileName().toString();
        String base = name.substring(0, name.length() - ".json".length());
        if (!base.equals("icons")) return base;
        Path parent = file.toAbsolutePath().getParent();
        return parent == null ? "preview" : parent.getFileName().toString();
    }
}
