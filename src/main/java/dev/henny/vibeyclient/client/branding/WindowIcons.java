package dev.henny.vibeyclient.client.branding;

import net.minecraft.server.packs.resources.IoSupplier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class WindowIcons {
    static final int[] SIZES = {16, 32, 48, 128, 256};

    private WindowIcons() {}

    public static List<IoSupplier<InputStream>> standard() {
        List<IoSupplier<InputStream>> out = new ArrayList<>();
        for (int size : SIZES) {
            String path = path(size);
            if (WindowIcons.class.getResource(path) == null) return null;
            out.add(() -> open(path));
        }
        return out;
    }

    static String path(int size) {
        return "/assets/vibeyclient/icons/icon_" + size + "x" + size + ".png";
    }

    private static InputStream open(String path) throws IOException {
        InputStream in = WindowIcons.class.getResourceAsStream(path);
        if (in == null) throw new FileNotFoundException(path);
        return in;
    }
}
