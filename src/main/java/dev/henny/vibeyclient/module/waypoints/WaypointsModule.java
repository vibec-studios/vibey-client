package dev.henny.vibeyclient.module.waypoints;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.waypoints.hud.WaypointHud;
import dev.henny.vibeyclient.module.waypoints.screen.Screens;
import dev.henny.vibeyclient.ui.Gfx;

import java.util.List;

public final class WaypointsModule extends VibeyModule {
    public static final String RULE_MINIMAP = "waypoints.minimap";

    private static volatile WaypointsModule instance;

    public final ActionSetting openList = action("openList", () -> Screens.list(null));
    public final ActionSetting openMap = action("openMap", Screens::map);

    public final BoolSetting beacons = bool("beacons", true).section("world");
    public final BoolSetting beams = bool("beams", true).section("world").visibleWhen(this::beaconsOn);
    public final BoolSetting deathMarker = bool("deathMarker", true).section("world");
    public final BoolSetting otherDimension = bool("otherDimension", true).section("world");
    public final BoolSetting portalCoords = bool("portalCoords", true).section("world");

    public final BoolSetting minimap = bool("minimap", false).section("minimap").serverRule(RULE_MINIMAP);
    public final BoolSetting minimapRotate = bool("minimapRotate", true).section("minimap").serverRule(RULE_MINIMAP)
            .visibleWhen(this::minimapOn);
    public final IntSetting minimapZoom = intRange("minimapZoom", 2, 1, 4).section("minimap").serverRule(RULE_MINIMAP)
            .visibleWhen(this::minimapOn);

    public final KeybindSetting addKey = keybind("addKey", Keys.J).onPress(() -> runtime().actions().addHere());
    public final KeybindSetting mapKey = keybind("mapKey", Keys.M).onPress(Screens::map);
    public final KeybindSetting minimapKey = keybind("minimapKey", Keys.I).serverRule(RULE_MINIMAP)
            .onPress(() -> minimap.set(!minimap.get()));
    public final KeybindSetting listKey = keybind("listKey", Keys.UNBOUND).onPress(() -> Screens.list(null));

    public final IntSetting cacheMb = intRange("cacheMb", 128, 16, 1024, 16).unit(" MB").advanced();

    private final WaypointsRuntime runtime = new WaypointsRuntime(this);

    public WaypointsModule() {
        super("waypoints", ModuleCategory.MISC);
        instance = this;
    }

    public static WaypointsModule get() {
        return instance;
    }

    public WaypointsRuntime runtime() {
        return runtime;
    }

    @Override
    public String icon() {
        return WaypointIcons.PIN;
    }

    @Override
    public int accentColor() {
        return 0xFF3FE0C5;
    }

    @Override
    public List<String> serverRuleKeys() {
        return List.of(RULE_MINIMAP);
    }

    public boolean minimapAllowed() {
        return minimap.get() && !ServerRules.isDisabled(RULE_MINIMAP);
    }

    private boolean beaconsOn() {
        return beacons.get();
    }

    private boolean minimapOn() {
        return minimap.get();
    }

    @Override
    public void onLoad() {
        WaypointIcons.register();
        ServerRules.supports(RULE_MINIMAP);
        WaypointHud.register(this);
        minimap.onChange(on -> {
            if (on) WaypointHud.showMinimap();
        });
        runtime.load();
    }

    @Override
    public void onEnable() {
        runtime.enable();
    }

    @Override
    public void onDisable() {
        runtime.disable();
    }

    @Override
    public void tick() {
        runtime.tick();
    }

    @Override
    public void renderHud(Gfx g) {
        runtime.renderHud(g);
    }

    @Override
    public void onWorldJoin() {
        runtime.join();
    }

    @Override
    public void onWorldLeave() {
        runtime.leave();
    }
}
