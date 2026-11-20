package dev.henny.vibeyclient.module.screens.store;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.module.screens.core.LayoutCodec;
import dev.henny.vibeyclient.module.screens.core.LayoutStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class LayoutFile {
    static final String NAME = "screens-layout.json";
    private static final ScheduledExecutorService IO = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Vibey screens save");
        t.setDaemon(true);
        return t;
    });
    private static ScheduledFuture<?> pending;

    private LayoutFile() {}

    public static Path path() {
        return SettingsGroup.configDir().resolve(NAME);
    }

    public static void load(LayoutStore store) {
        Path p = path();
        if (!Files.isRegularFile(p)) return;
        try {
            store.load(LayoutCodec.read(Files.readString(p, StandardCharsets.UTF_8)));
        } catch (IOException | RuntimeException e) {
            Vibey.LOG.warn("Could not read {}", p, e);
        }
    }

    public static synchronized void saveSoon(LayoutStore store) {
        if (pending != null) pending.cancel(false);
        pending = IO.schedule(() -> write(store), 400, TimeUnit.MILLISECONDS);
    }

    private static void write(LayoutStore store) {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            Path tmp = p.resolveSibling(NAME + ".tmp");
            Files.writeString(tmp, LayoutCodec.write(store.snapshot()).toString(), StandardCharsets.UTF_8);
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException e) {
            Vibey.LOG.warn("Could not save {}", p, e);
        }
    }
}
