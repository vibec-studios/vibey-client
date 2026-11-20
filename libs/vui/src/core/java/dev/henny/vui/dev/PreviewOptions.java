package dev.henny.vui.dev;

import java.nio.file.Path;

public record PreviewOptions(Path file, int width, int height, Path output, Path state, int scale, Path root, int servePort,
                             boolean serve) {

    public static final String USAGE = """
            usage: Preview <file.vui> <width> <height> <out.png> [state.json] [--scale N] [--root DIR] [--serve [PORT]]
              file.vui    a .vui or .vui.json file
              width/height GUI pixels (e.g. 427 240 = smallest Minecraft screen)
              out.png     where the screenshot goes
              state.json  optional JSON object used as the controller (keys become values, arrays become lists)
              --scale N   pixels per GUI pixel in the PNG (default 2)
              --root DIR  resource root holding assets/<ns>/vui/ for imports (default: derived from the file path)
              --serve     keep running: watch the file, serve the dev bridge on localhost (default port 7591)
            """;

    public static PreviewOptions parse(String[] args) {
        if (args.length < 4) throw new IllegalArgumentException(USAGE);
        Path file = Path.of(args[0]);
        int width = Integer.parseInt(args[1]);
        int height = Integer.parseInt(args[2]);
        Path out = Path.of(args[3]);
        Path state = null;
        int scale = 2;
        Path root = null;
        int port = DevBridge.DEFAULT_PORT;
        boolean serve = false;
        for (int i = 4; i < args.length; i++) {
            switch (args[i]) {
                case "--scale" -> scale = Integer.parseInt(args[++i]);
                case "--root" -> root = Path.of(args[++i]);
                case "--serve" -> {
                    serve = true;
                    if (i + 1 < args.length && args[i + 1].matches("\\d+")) port = Integer.parseInt(args[++i]);
                }
                default -> {
                    if (state != null) throw new IllegalArgumentException("unexpected argument " + args[i] + "\n" + USAGE);
                    state = Path.of(args[i]);
                }
            }
        }
        return new PreviewOptions(file, width, height, out, state, scale, root, port, serve);
    }
}
