package dev.henny.vibeyclient.gametest;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

final class WorldFixtures {
    private WorldFixtures() {}

    static Path saves(Minecraft mc) {
        return mc.getLevelSource().getBaseDir();
    }

    static boolean exists(Minecraft mc, String id) {
        return Files.isRegularFile(saves(mc).resolve(id).resolve("level.dat"));
    }

    static void copyFirst(Minecraft mc, String id, String name, long lastPlayed) {
        Path target = saves(mc).resolve(id);
        try {
            if (!Files.isDirectory(target)) copy(source(mc).orElseThrow(() -> new AssertionError("no world to copy")), target);
            Path dat = target.resolve("level.dat");
            CompoundTag root = NbtIo.readCompressed(dat, NbtAccounter.unlimitedHeap());
            CompoundTag data = root.getCompoundOrEmpty("Data");
            data.putString("LevelName", name);
            data.putLong("LastPlayed", lastPlayed);
            root.put("Data", data);
            NbtIo.writeCompressed(root, dat);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<Path> source(Minecraft mc) throws IOException {
        try (Stream<Path> s = Files.list(saves(mc))) {
            return s.filter(p -> Files.isRegularFile(p.resolve("level.dat")))
                    .filter(p -> !p.getFileName().toString().startsWith("vibey_dnd_")).findFirst();
        }
    }

    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> s = Files.walk(from)) {
            for (Path p : s.sorted(Comparator.naturalOrder()).toList()) {
                if (p.getFileName().toString().equals("session.lock")) continue;
                Path dest = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(dest);
                else Files.copy(p, dest);
            }
        }
    }
}
