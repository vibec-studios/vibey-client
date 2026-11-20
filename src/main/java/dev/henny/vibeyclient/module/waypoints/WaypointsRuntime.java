package dev.henny.vibeyclient.module.waypoints;

import dev.henny.r3.mc.R3Mod;
import dev.henny.vibeyclient.module.waypoints.actions.DeathWatcher;
import dev.henny.vibeyclient.module.waypoints.actions.Navigator;
import dev.henny.vibeyclient.module.waypoints.actions.WaypointActions;
import dev.henny.vibeyclient.module.waypoints.beacon.BeaconLayer;
import dev.henny.vibeyclient.module.waypoints.data.WaypointStore;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import dev.henny.vibeyclient.module.waypoints.hud.NavArrow;
import dev.henny.vibeyclient.module.waypoints.hud.PortalLook;
import dev.henny.vibeyclient.module.waypoints.map.MapService;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;

public final class WaypointsRuntime {
    private final WaypointsModule module;
    private final WaypointStore store = new WaypointStore();
    private final Navigator navigator = new Navigator();
    private final WaypointActions actions;
    private final DeathWatcher death = new DeathWatcher();
    private final MapService map = new MapService();
    private final BeaconLayer beacons;
    private boolean enabled;
    private String worldKey;

    WaypointsRuntime(WaypointsModule module) {
        this.module = module;
        this.actions = new WaypointActions(this);
        this.beacons = new BeaconLayer(this);
    }

    public static WaypointsRuntime get() {
        WaypointsModule m = WaypointsModule.get();
        return m == null ? null : m.runtime();
    }

    public WaypointsModule module() {
        return module;
    }

    public WaypointStore store() {
        return store;
    }

    public Navigator navigator() {
        return navigator;
    }

    public WaypointActions actions() {
        return actions;
    }

    public MapService map() {
        return map;
    }

    public BeaconLayer beacons() {
        return beacons;
    }

    public boolean active() {
        return enabled && worldKey != null;
    }

    void load() {
        R3Mod.whenReady(beacons::attach);
    }

    void enable() {
        enabled = true;
    }

    void disable() {
        enabled = false;
        leave();
    }

    void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            leave();
            return;
        }
        String key = WorldContext.worldKey(mc);
        if (!key.equals(worldKey)) {
            leave();
            join(key);
        }
        if (module.deathMarker.get()) death.tick(mc, actions);
        navigator.tick(mc, store.list());
        map.tick(mc);
        beacons.sync(mc);
    }

    void join() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && worldKey == null) join(WorldContext.worldKey(mc));
    }

    private void join(String key) {
        worldKey = key;
        store.load(key);
        map.open(key, module.cacheMb.get() * 1024L * 1024L);
        navigator.clear();
    }

    void leave() {
        if (worldKey == null) return;
        store.unload();
        map.close();
        beacons.clear();
        navigator.clear();
        worldKey = null;
    }

    void renderHud(Gfx g) {
        if (!active()) return;
        NavArrow.draw(g, this);
        if (module.portalCoords.get()) PortalLook.draw(g);
    }
}
