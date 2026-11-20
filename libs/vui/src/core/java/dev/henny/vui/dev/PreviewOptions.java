package dev.henny.vui.dev;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record PreviewOptions(Path file, int width, int height, Path output, Path state, int scale, Path root, int servePort,
                             boolean serve, List<Path> lang, Path theme, List<Path> icons, List<Path> textures,
                             List<PreviewStep> steps, boolean quiet) {

    public static final String USAGE = """
            usage: Preview <file.vui> <width> <height> <out.png> [state.json] [options]
              file.vui       a .vui or .vui.json file
              width/height   GUI pixels (e.g. 427 240 = smallest Minecraft screen)
              out.png        where the screenshot goes
              state.json     optional JSON object used as the controller (keys become values, arrays become lists)
              --scale N      pixels per GUI pixel in the PNG (default 2)
              --root DIR     resource root holding assets/<ns>/vui/ for imports (default: derived from the file path)
              --lang PATH    language JSON file or folder (file name = key prefix: ui.json -> ui.*); repeat for fallbacks
              --theme FILE   theme file (JSON or $token lines) on top of the default theme
              --icons PATH   extra icons.json file or folder (namespace from assets/<ns>/ or the file name)
              --textures DIR extra texture root (assets/<ns>/textures/...), repeatable
              --hover AT     move the pointer to X,Y (GUI px), an element id or a selector; tooltips show in the shot
              --scroll N     wheel N lines down (negative = up) at the pointer or the centre
              --click ID     click the element with that id (or a selector like .tile, or X,Y)
              --wait S       let S seconds of animation run
              --quiet        do not print the element tree
              --serve [PORT] keep running: watch files, serve the dev bridge on localhost (default port 7591)
            Steps (--hover, --scroll, --click, --wait) run in the given order; then animations settle.
            """;

    public PreviewOptions(Path file, int width, int height, Path output, Path state, int scale, Path root, int servePort,
                          boolean serve) {
        this(file, width, height, output, state, scale, root, servePort, serve, List.of(), null, List.of(), List.of(),
                List.of(), false);
    }

    public static PreviewOptions parse(String[] args) {
        if (args.length < 4) throw new IllegalArgumentException(USAGE);
        Builder b = new Builder();
        for (int i = 4; i < args.length; i++) i = b.option(args, i);
        return new PreviewOptions(Path.of(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Path.of(args[3]),
                b.state, b.scale, b.root, b.port, b.serve, List.copyOf(b.lang), b.theme, List.copyOf(b.icons),
                List.copyOf(b.textures), List.copyOf(b.steps), b.quiet);
    }

    private static final class Builder {
        Path state;
        int scale = 2;
        Path root;
        int port = DevBridge.DEFAULT_PORT;
        boolean serve;
        boolean quiet;
        Path theme;
        final List<Path> lang = new ArrayList<>();
        final List<Path> icons = new ArrayList<>();
        final List<Path> textures = new ArrayList<>();
        final List<PreviewStep> steps = new ArrayList<>();

        int option(String[] a, int i) {
            switch (a[i]) {
                case "--scale" -> scale = Integer.parseInt(value(a, ++i));
                case "--root" -> root = Path.of(value(a, ++i));
                case "--lang" -> lang.add(Path.of(value(a, ++i)));
                case "--theme" -> theme = Path.of(value(a, ++i));
                case "--icons" -> icons.add(Path.of(value(a, ++i)));
                case "--textures" -> textures.add(Path.of(value(a, ++i)));
                case "--hover" -> steps.add(PreviewStep.hover(value(a, ++i)));
                case "--scroll" -> steps.add(new PreviewStep.Scroll(Float.parseFloat(value(a, ++i))));
                case "--click" -> steps.add(new PreviewStep.Click(value(a, ++i)));
                case "--wait" -> steps.add(new PreviewStep.Wait(Float.parseFloat(value(a, ++i))));
                case "--quiet" -> quiet = true;
                case "--serve" -> {
                    serve = true;
                    if (i + 1 < a.length && a[i + 1].matches("\\d+")) port = Integer.parseInt(a[++i]);
                }
                default -> {
                    if (state != null || a[i].startsWith("--")) throw new IllegalArgumentException("unexpected argument " + a[i] + "\n" + USAGE);
                    state = Path.of(a[i]);
                }
            }
            return i;
        }

        private static String value(String[] a, int i) {
            if (i >= a.length) throw new IllegalArgumentException(a[i - 1] + " needs a value\n" + USAGE);
            return a[i];
        }
    }
}
