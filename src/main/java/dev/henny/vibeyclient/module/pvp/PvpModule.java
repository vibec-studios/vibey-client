package dev.henny.vibeyclient.module.pvp;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.ColorSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.HudLayout;

/**
 * PvP basics and view tweaks (docs/PVP.md): zoom, toggle sprint/sneak with a HUD indicator, a crosshair editor,
 * block outline, hit colour, low fire and a lower shield. Nothing here gives more than vanilla allows: zoom is a
 * spyglass-like view, toggles only press the vanilla keys for you, the rest is cosmetic. Every feature has a
 * {@link ServerRules} key and switches itself off while the server disables it.
 *
 * <p>Fabric 1.21.11 only for now: gradle/common.gradle leaves the package (and its mixin config) out of every other
 * target and {@code BuiltinModules} registers it by reflection.
 */
public final class PvpModule extends VibeyModule {
    /** Server-rule keys (vibe-launcher docs/PLAY-LINKS.md §5). */
    public static final String RULE_ZOOM = "zoom", RULE_SPRINT = "togglesprint", RULE_SNEAK = "togglesneak",
            RULE_CROSSHAIR = "crosshair", RULE_OUTLINE = "blockoutline", RULE_HIT = "hitcolor", RULE_FIRE = "lowfire",
            RULE_SHIELD = "lowshield", RULE_HUD = "hud.togglestate";
    static final String[] RULES = {RULE_ZOOM, RULE_SPRINT, RULE_SNEAK, RULE_CROSSHAIR, RULE_OUTLINE, RULE_HIT,
            RULE_FIRE, RULE_SHIELD, RULE_HUD};

    private static final int[] CROSSHAIR_COLORS = {0xFFFFFFFF, 0xFF000000, 0xFFFF3B3B, 0xFF3BFF6B, 0xFF3FE0C5,
            0xFFFFE14D, 0xFFFF5CE1, 0xFF8B6CFF};
    private static final int[] OUTLINE_COLORS = {0xFF000000, 0xFFFFFFFF, 0xFF8B6CFF, 0xFF3FE0C5, 0xFFFF3B3B,
            0xFFFFE14D, 0xFF3BFF6B};
    private static final int[] HIT_COLORS = {0xFFFF0000, 0xFFFF7A00, 0xFFFFE14D, 0xFF3BFF6B, 0xFF3FE0C5, 0xFF4D7BFF,
            0xFF8B6CFF, 0xFFFFFFFF};

    private static volatile PvpModule instance;

    // ---------------------------------------------------------------------------------------- buttons (page header)
    public final ActionSetting editCrosshair = action("editCrosshair",
            () -> ClientCompat.openScreen(new CrosshairEditorScreen(ClientCompat.screen(), this)));
    public final ActionSetting editHud = action("editHud", () -> HudLayout.openEditor(ClientCompat.screen()));

    // ---------------------------------------------------------------------------------------- zoom
    public final BoolSetting zoom = bool("zoom", true).section("zoom").serverRule(RULE_ZOOM);
    public final FloatSetting zoomLevel = floatRange("zoomLevel", 4f, 2f, 10f, 0.5f).decimals(1)
            .section("zoom").serverRule(RULE_ZOOM).visibleWhen(this::zoomOn);
    public final BoolSetting zoomSmooth = bool("zoomSmooth", true).section("zoom").serverRule(RULE_ZOOM).visibleWhen(this::zoomOn);
    public final BoolSetting zoomScroll = bool("zoomScroll", true).section("zoom").serverRule(RULE_ZOOM).visibleWhen(this::zoomOn);
    public final BoolSetting zoomCinematic = bool("zoomCinematic", false).section("zoom").serverRule(RULE_ZOOM).visibleWhen(this::zoomOn);
    public final KeybindSetting zoomKey = keybind("zoomKey", Keys.C).serverRule(RULE_ZOOM);

    // ---------------------------------------------------------------------------------------- movement
    public final BoolSetting toggleSprint = bool("toggleSprint", false).section("movement").serverRule(RULE_SPRINT);
    public final BoolSetting toggleSneak = bool("toggleSneak", false).section("movement").serverRule(RULE_SNEAK);
    public final BoolSetting showToggleState = bool("showToggleState", true).section("movement").serverRule(RULE_HUD)
            .visibleWhen(() -> toggleSprint.on() || toggleSneak.on());

    // ---------------------------------------------------------------------------------------- crosshair
    public final EnumSetting<Crosshair.Shape> crosshair = choice("crosshair", Crosshair.Shape.VANILLA)
            .section("crosshair").serverRule(RULE_CROSSHAIR);
    public final IntSetting chSize = intRange("chSize", 5, 1, 12).section("crosshair").serverRule(RULE_CROSSHAIR)
            .visibleWhen(this::customCrosshair);
    public final IntSetting chGap = intRange("chGap", 2, 0, 8).section("crosshair").serverRule(RULE_CROSSHAIR)
            .visibleWhen(this::customCrosshair);
    public final IntSetting chThickness = intRange("chThickness", 1, 1, 5).section("crosshair").serverRule(RULE_CROSSHAIR)
            .visibleWhen(this::customCrosshair);
    public final ColorSetting chColor = color("chColor", 0xFFFFFFFF).presets(CROSSHAIR_COLORS).section("crosshair")
            .serverRule(RULE_CROSSHAIR).visibleWhen(this::customCrosshair);
    public final IntSetting chOpacity = intRange("chOpacity", 100, 20, 100, 5).unit("%").section("crosshair")
            .serverRule(RULE_CROSSHAIR).visibleWhen(this::customCrosshair);
    public final BoolSetting chOutline = bool("chOutline", true).section("crosshair").serverRule(RULE_CROSSHAIR)
            .visibleWhen(this::customCrosshair);
    public final BoolSetting chDot = bool("chDot", false).section("crosshair").serverRule(RULE_CROSSHAIR)
            .visibleWhen(this::customCrosshair);
    public final BoolSetting chDynamic = bool("chDynamic", false).section("crosshair").serverRule(RULE_CROSSHAIR)
            .visibleWhen(this::customCrosshair);
    public final BoolSetting chInvert = bool("chInvert", false).section("crosshair").serverRule(RULE_CROSSHAIR)
            .visibleWhen(this::customCrosshair);

    // ---------------------------------------------------------------------------------------- block outline
    public final BoolSetting outline = bool("outline", false).section("outline").serverRule(RULE_OUTLINE);
    public final ColorSetting outlineColor = color("outlineColor", 0xFF8B6CFF).presets(OUTLINE_COLORS).section("outline")
            .serverRule(RULE_OUTLINE).visibleWhen(this::outlineOn);
    public final FloatSetting outlineWidth = floatRange("outlineWidth", 3.5f, 1f, 6f, 0.5f).decimals(1).section("outline")
            .serverRule(RULE_OUTLINE).visibleWhen(this::outlineOn);
    public final IntSetting outlineOpacity = intRange("outlineOpacity", 100, 20, 100, 5).unit("%").section("outline")
            .serverRule(RULE_OUTLINE).visibleWhen(this::outlineOn);
    public final BoolSetting outlineFill = bool("outlineFill", false).section("outline").serverRule(RULE_OUTLINE)
            .visibleWhen(this::outlineOn);
    public final IntSetting outlineFillOpacity = intRange("outlineFillOpacity", 20, 5, 60, 5).unit("%").section("outline")
            .serverRule(RULE_OUTLINE).visibleWhen(() -> outlineOn() && outlineFill.on());

    // ---------------------------------------------------------------------------------------- hit colour
    public final BoolSetting hitColor = bool("hitColor", false).section("hit").serverRule(RULE_HIT);
    public final ColorSetting hitTint = color("hitTint", 0xFFFF0000).presets(HIT_COLORS).section("hit")
            .serverRule(RULE_HIT).visibleWhen(hitColor::on);
    public final IntSetting hitStrength = intRange("hitStrength", 30, 10, 80, 5).unit("%").section("hit")
            .serverRule(RULE_HIT).visibleWhen(hitColor::on);

    // ---------------------------------------------------------------------------------------- first person
    public final BoolSetting lowFire = bool("lowFire", false).section("view").serverRule(RULE_FIRE);
    public final IntSetting fireHeight = intRange("fireHeight", 70, 10, 100, 5).unit("%").section("view")
            .serverRule(RULE_FIRE).visibleWhen(lowFire::on);
    public final BoolSetting lowShield = bool("lowShield", false).section("view").serverRule(RULE_SHIELD);
    public final IntSetting shieldHeight = intRange("shieldHeight", 50, 10, 100, 5).unit("%").section("view")
            .serverRule(RULE_SHIELD).visibleWhen(lowShield::on);

    // ---------------------------------------------------------------------------------------- advanced
    public final IntSetting zoomTime = intRange("zoomTime", 140, 40, 400, 10).unit(" ms").advanced();
    public final BoolSetting zoomSensitivity = bool("zoomSensitivity", true).advanced();
    public final FloatSetting zoomScrollStep = floatRange("zoomScrollStep", 1.25f, 1.05f, 2f, 0.05f).advanced();
    public final IntSetting chDynamicAmount = intRange("chDynamicAmount", 4, 1, 10).advanced();

    public PvpModule() {
        super("pvp", ModuleCategory.MISC);
        instance = this;
    }

    /** The module while it exists (null on builds without it or before construction). */
    public static PvpModule get() {
        return instance;
    }

    @Override
    public int accentColor() {
        return 0xFFFF5470;
    }

    /** Tile line and page notice "Disabled by this server: …" (ui/ServerBadge). */
    @Override
    public java.util.List<String> serverRuleKeys() {
        return java.util.List.of(RULES);
    }

    @Override
    public void onLoad() {
        for (String key : RULES) ServerRules.supports(key);
        ServerRules.listen(this::onRulesChanged);
        PvpHud.register(this);
        hitColor.onChange(v -> HitColor.refresh());
        hitTint.onChange(v -> HitColor.refresh());
        hitStrength.onChange(v -> HitColor.refresh());
        toggleSprint.onChange(v -> ToggleMovement.reset());
        toggleSneak.onChange(v -> ToggleMovement.reset());
        zoom.onChange(v -> Zoom.reset());
    }

    @Override
    public void onEnable() {
        HitColor.refresh();
    }

    @Override
    public void onDisable() {
        Zoom.reset();
        ToggleMovement.reset();
        HitColor.refresh();
    }

    @Override
    public void tick() {
        ToggleMovement.tick(this);
    }

    @Override
    public void renderHud(Gfx g) {
        // the toggle-state widget is drawn by the core HUD widget system (PvpHud)
    }

    @Override
    public void onWorldLeave() {
        Zoom.reset();
        ToggleMovement.reset();
    }

    private void onRulesChanged() {
        if (!zoomActive()) Zoom.reset();
        if (!sprintActive() || !sneakActive()) ToggleMovement.reset();
        HitColor.refresh();
    }

    // ============================================================================================ effective state

    private boolean zoomOn() {
        return zoom.on();
    }

    private boolean outlineOn() {
        return outline.on();
    }

    private boolean customCrosshair() {
        return crosshair.get() != Crosshair.Shape.VANILLA;
    }

    /** Feature on in the settings, module on, and the server allows it. */
    private boolean active(BoolSetting s, String rule) {
        return isEnabled() && s.on() && !ServerRules.isDisabled(rule);
    }

    public boolean zoomActive() {
        return active(zoom, RULE_ZOOM);
    }

    public boolean sprintActive() {
        return active(toggleSprint, RULE_SPRINT);
    }

    public boolean sneakActive() {
        return active(toggleSneak, RULE_SNEAK);
    }

    public boolean hudActive() {
        return isEnabled() && showToggleState.on() && !ServerRules.isDisabled(RULE_HUD);
    }

    public boolean crosshairActive() {
        return isEnabled() && customCrosshair() && !ServerRules.isDisabled(RULE_CROSSHAIR);
    }

    public boolean outlineActive() {
        return active(outline, RULE_OUTLINE);
    }

    public boolean hitColorActive() {
        return active(hitColor, RULE_HIT);
    }

    public boolean lowFireActive() {
        return active(lowFire, RULE_FIRE);
    }

    public boolean lowShieldActive() {
        return active(lowShield, RULE_SHIELD);
    }

    /** The current crosshair settings as a style (cheap, used every frame). */
    public Crosshair.Style crosshairStyle(Crosshair.Style out) {
        out.shape = crosshair.get();
        out.size = chSize.get();
        out.gap = chGap.get();
        out.thickness = chThickness.get();
        out.color = (chColor.argb() & 0xFFFFFF) | (Math.round(chOpacity.get() * 2.55f) << 24);
        out.outline = chOutline.on();
        out.dot = chDot.on();
        out.invert = chInvert.on();
        return out;
    }
}
