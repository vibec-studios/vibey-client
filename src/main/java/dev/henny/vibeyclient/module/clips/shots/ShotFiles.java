package dev.henny.vibeyclient.module.clips.shots;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.clips.shots.core.Favourites;
import dev.henny.vibeyclient.module.clips.shots.core.Shot;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShotFiles {
    private static volatile ShotFolder override;

    private ShotFiles() {}

    public static void useFolder(ShotFolder folder) {
        override = folder;
    }

    public static Path dir() {
        ShotFolder o = override;
        return o != null ? o.screenshots() : FabricLoader.getInstance().getGameDir().resolve("screenshots");
    }

    public static Path favouritesFile() {
        ShotFolder o = override;
        return o != null ? o.favourites() : FabricLoader.getInstance().getGameDir().resolve("vibeyclient").resolve("screenshots.json");
    }

    static Path cacheDir() {
        return FabricLoader.getInstance().getGameDir().resolve("vibeyclient").resolve("cache").resolve("shots");
    }

    public static Path file(Shot s) {
        return dir().resolve(s.name());
    }

    static List<Shot> scan() {
        List<Shot> out = new ArrayList<>();
        Path d = dir();
        if (!Files.isDirectory(d)) return out;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(d)) {
            for (Path p : files) {
                String n = p.getFileName().toString();
                if (!image(n)) continue;
                BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);
                if (a.isRegularFile()) out.add(new Shot(n, a.lastModifiedTime().toMillis(), a.size()));
            }
        } catch (IOException e) {
            Vibey.LOG.warn("Screenshots: can't list {}: {}", d, e.toString());
        }
        return out;
    }

    static Favourites loadFavourites() {
        try {
            Path f = favouritesFile();
            return Files.exists(f) ? Favourites.fromJson(Files.readString(f, StandardCharsets.UTF_8)) : new Favourites();
        } catch (IOException e) {
            return new Favourites();
        }
    }

    static void saveFavourites(String json) {
        Path f = favouritesFile();
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(".screenshots.json.tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Vibey.LOG.warn("Screenshots: can't save favourites: {}", e.toString());
        }
    }

    static boolean delete(Shot s) {
        try {
            return Files.deleteIfExists(file(s));
        } catch (IOException e) {
            Vibey.LOG.warn("Screenshots: can't delete {}: {}", s.name(), e.toString());
            return false;
        }
    }

    public static void openFolder() {
        Path d = dir();
        try {
            Files.createDirectories(d);
        } catch (IOException ignored) {
            Vibey.LOG.debug("Screenshots: folder {} not created", d);
        }
        ClientCompat.openFolder(d);
    }

    private static boolean image(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return !n.startsWith(".") && (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg"));
    }
}
