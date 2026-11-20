package dev.henny.vibeyclient.module.media.video;

import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.persist.PlacementCodec;
import dev.henny.r3.persist.PlacementRecord;
import dev.henny.r3.persist.PlacementStore;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.media.video.world.ScreenObjects;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ScreenStore {
    private final Video video;

    ScreenStore(Video video) {
        this.video = video;
    }

    public void load() {
        String key = R3Mc.worldKey();
        if (key == null) return;
        PlacementStore store = R3Mc.placements();
        migrate(store, key);
        UUID me = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        for (PlacementRecord r : store.list(key, ScreenObjects.KIND)) {
            try {
                MediaScreen s = new MediaScreen(UUID.fromString(r.id));
                s.owner = me;
                s.ownerName = ClientCompat.playerName();
                ScreenData.apply(r.data, s, false);
                s.glowing = r.fullBright;
                WorldObject o = PlacementCodec.decode(r, ScreenObjects.LIMITS);
                ScreenObjects.dress(o, s);
                R3Mc.add(o);
                video.screens.put(s.id, s);
            } catch (RuntimeException e) {
                Vibey.LOG.warn("Skipping a saved video screen: {}", e.toString());
            }
        }
    }

    public void save() {
        String key = R3Mc.worldKey();
        if (key == null) return;
        List<PlacementRecord> own = new ArrayList<>();
        for (MediaScreen s : video.screens.values()) {
            if (s.remote || !s.placed()) continue;
            s.object.owner(s.owner);
            own.add(PlacementCodec.encode(s.object, ScreenData.encode(s, false)));
        }
        R3Mc.placements().replaceAll(key, ScreenObjects.KIND, own);
        R3Mc.storage().saveWorld();
    }

    private void migrate(PlacementStore store, String key) {
        List<PlacementRecord> old = LegacyScreens.take(video.cfg.data(), key);
        if (old.isEmpty()) return;
        for (PlacementRecord r : old) store.put(key, r);
        video.cfg.markDirty();
        R3Mc.storage().saveWorld();
        Vibey.LOG.info("Moved {} saved video screens to r3 placements ({})", old.size(), key);
    }
}
