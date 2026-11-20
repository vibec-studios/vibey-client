package dev.henny.vibeyclient.module.waypoints.data;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.setting.SettingsGroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class StoreFiles {
    private StoreFiles() {}

    public static Path waypoints(String worldKey) {
        return SettingsGroup.configDir().resolve("waypoints").resolve(WorldContext.safe(worldKey) + ".json");
    }

    public static Path mapsRoot() {
        return Vibey.platform().gameDir().resolve("vibeyclient").resolve("maps");
    }

    public static Path maps(String worldKey) {
        return mapsRoot().resolve(WorldContext.safe(worldKey));
    }

    public static void writeAtomically(Path file, byte[] data) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, data);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
