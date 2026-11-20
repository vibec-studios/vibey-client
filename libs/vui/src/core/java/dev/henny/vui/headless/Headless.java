package dev.henny.vui.headless;

import dev.henny.vui.Vui;
import dev.henny.vui.dom.Document;
import dev.henny.vui.render.Backend;

import java.io.IOException;
import java.nio.file.Path;

public final class Headless {
    public static final int BACKGROUND = 0xFF000000;

    private final HeadlessInput input = new HeadlessInput();
    private final HeadlessTextures textures = new HeadlessTextures();
    private final PixelFontMetrics metrics = new PixelFontMetrics();
    private final Backend backend = new Backend(metrics, input, textures);

    public static Headless create() {
        return new Headless();
    }

    public Backend backend() {
        return backend;
    }

    public HeadlessInput input() {
        return input;
    }

    public HeadlessTextures textures() {
        return textures;
    }

    public Vui vui() {
        return new Vui(backend);
    }

    public ImageCanvas render(Document doc, int width, int height, int scale) {
        doc.resize(width, height);
        doc.frame(0);
        ImageCanvas canvas = new ImageCanvas(width, height, scale, textures, BACKGROUND);
        doc.render(canvas);
        return canvas;
    }

    public void screenshot(Document doc, int width, int height, int scale, Path out) throws IOException {
        render(doc, width, height, scale).write(out);
    }

    public byte[] png(Document doc, int scale) throws IOException {
        ImageCanvas canvas = new ImageCanvas(Math.max(1, (int) doc.width()), Math.max(1, (int) doc.height()), scale, textures, BACKGROUND);
        doc.render(canvas);
        return canvas.png();
    }
}
