package dev.henny.vibeyclient.module.servers.worlds;

import dev.henny.vibeyclient.module.servers.ServersModule;
import dev.henny.vibeyclient.module.servers.core.WorldOrder;
import net.minecraft.world.level.storage.LevelSummary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorldListView {
    private static List<String> full = List.of();

    private WorldListView() {}

    public static List<String> full() {
        return full;
    }

    public static List<LevelSummary> arrange(List<LevelSummary> in) {
        ServersModule m = ServersModule.get();
        if (m == null || !m.worldOrderActive() || in == null) return in;
        Map<String, LevelSummary> byId = new LinkedHashMap<>();
        List<WorldOrder.World> worlds = new ArrayList<>();
        for (LevelSummary s : in) {
            if (byId.putIfAbsent(s.getLevelId(), s) == null) {
                worlds.add(new WorldOrder.World(s.getLevelId(), s.getLevelName(), s.getLastPlayed()));
            }
        }
        List<String> ids = WorldOrder.arrange(worlds, WorldOrderStore.get(), m.worldSort.get());
        full = List.copyOf(ids);
        List<LevelSummary> out = new ArrayList<>(ids.size());
        for (String id : ids) out.add(byId.get(id));
        return out;
    }
}
