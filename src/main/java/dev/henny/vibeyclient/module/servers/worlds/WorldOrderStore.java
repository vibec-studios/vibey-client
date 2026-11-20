package dev.henny.vibeyclient.module.servers.worlds;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.module.servers.core.OrderCodec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class WorldOrderStore {
    static final String FILE = "world-order.json";
    private static List<String> order;

    private WorldOrderStore() {}

    public static synchronized List<String> get() {
        if (order == null) order = load();
        return order;
    }

    public static synchronized void set(List<String> next) {
        order = List.copyOf(next);
        save(order);
    }

    public static synchronized void reload() {
        order = null;
    }

    static Path file() {
        return SettingsGroup.configDir().resolve(FILE);
    }

    private static List<String> load() {
        Path f = file();
        if (!Files.isRegularFile(f)) return List.of();
        try {
            return List.copyOf(OrderCodec.read(JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8))));
        } catch (IOException | RuntimeException e) {
            Vibey.LOG.warn("Could not read {}: {}", f, e.toString());
            return List.of();
        }
    }

    private static void save(List<String> ids) {
        Path f = file();
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(FILE + ".tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(OrderCodec.write(ids)), StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Vibey.LOG.warn("Could not save {}: {}", f, e.toString());
        }
    }
}
