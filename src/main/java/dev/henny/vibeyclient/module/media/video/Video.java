package dev.henny.vibeyclient.module.media.video;

import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.mc.R3Mod;
import dev.henny.r3.mc.client.WorldListener;
import dev.henny.r3.interact.Interactable;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.media.video.net.VideoSync;
import dev.henny.vibeyclient.module.media.video.play.Players;
import dev.henny.vibeyclient.module.media.video.world.ScreenEvents;
import dev.henny.vibeyclient.module.media.video.world.ScreenObjects;
import dev.henny.vibeyclient.module.media.video.world.ScreenSurfaces;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Video {
    private static final long SAVE_EVERY_MS = 30_000;
    private static Video instance;

    public final VideoFeature cfg;
    public final Map<UUID, MediaScreen> screens = new LinkedHashMap<>();
    public final Players players = new Players(this);
    public final ScreenStatus status = new ScreenStatus(this);
    public final ScreenActions actions = new ScreenActions(this);
    public final ScreenStore store = new ScreenStore(this);
    public final VideoSync net = new VideoSync(this);
    public final OverlayView overlay = new OverlayView(this);
    private final ScreenSurfaces surfaces = new ScreenSurfaces(this);
    private boolean enabled;
    private boolean hooks;
    private boolean inWorld;
    public UUID overlayId;
    private long lastSave;

    public Video(VideoFeature cfg) {
        this.cfg = cfg;
        instance = this;
    }

    public static Video get() {
        return instance;
    }

    public static String tr(String key, Object... args) {
        return Lang.tr("media.video." + key, args);
    }

    public void onLoad() {
        net.register();
        VideoDev.register(this);
        if (hooks) return;
        hooks = true;
        WorldRenderEvents.END_EXTRACTION.register(ctx -> {
            if (enabled) players.frame();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> players.closeAll());
        R3Mod.whenReady(() -> {
            R3Mc.scene().interaction().listen(new ScreenEvents(this));
            R3Mc.onWorld(new WorldListener() {
                @Override
                public void worldJoined(String worldKey) {
                    joined();
                }

                @Override
                public void worldLeft(String worldKey) {
                    left();
                }
            });
            if (R3Mc.worldKey() != null) joined();
        });
    }

    public void onEnable() {
        enabled = true;
        R3Mod.whenReady(() -> {
            if (R3Mc.worldKey() != null) joined();
        });
    }

    public void onDisable() {
        left();
        enabled = false;
    }

    private void joined() {
        if (!enabled || inWorld) return;
        inWorld = true;
        clear();
        store.load();
        net.onJoin();
    }

    private void left() {
        if (!inWorld) return;
        store.save();
        net.onLeave();
        clear();
        inWorld = false;
    }

    private void clear() {
        players.closeAll();
        for (MediaScreen s : screens.values()) unplace(s);
        screens.clear();
        overlayId = null;
    }

    public void unplace(MediaScreen s) {
        if (s.object == null) return;
        if (R3Mc.scene().interaction().held() == s.object) R3Mc.scene().interaction().cancel();
        R3Mc.scene().remove(s.object.id());
        ScreenObjects.close(s.object);
        s.object = null;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean inWorld() {
        return enabled && inWorld;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!inWorld() || mc.level == null || mc.player == null) return;
        players.tick();
        surfaces.tick();
        long now = System.currentTimeMillis();
        net.tick(now);
        if (now - lastSave > SAVE_EVERY_MS) {
            lastSave = now;
            store.save();
        }
    }

    public MediaScreen find(Interactable target) {
        if (!(target instanceof WorldObject object) || !ScreenObjects.KIND.equals(object.kind())) return null;
        for (MediaScreen s : screens.values()) if (s.object == object) return s;
        return null;
    }

    public String dimension() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? "" : mc.level.dimension().identifier().toString();
    }

    public MediaScreen overlayScreen() {
        return overlayId == null ? null : screens.get(overlayId);
    }
}
