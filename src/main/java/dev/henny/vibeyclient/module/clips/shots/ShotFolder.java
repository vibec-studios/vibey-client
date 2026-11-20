package dev.henny.vibeyclient.module.clips.shots;

import java.nio.file.Path;

public record ShotFolder(Path screenshots, Path favourites) {
    public static ShotFolder in(Path root) {
        return new ShotFolder(root.resolve("screenshots"), root.resolve("screenshots.json"));
    }
}
