package dev.henny.vibeyclient.module.info;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.info.card.CardHud;
import dev.henny.vibeyclient.module.info.food.HungerBarOverlay;
import dev.henny.vibeyclient.module.info.light.ChunkBorders;
import dev.henny.vibeyclient.module.info.tool.ToolWarnHud;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.HudLayout;

import java.util.List;

public final class InfoModule extends VibeyModule {
    private static volatile InfoModule instance;

    public final ActionSetting editHud = action("editHud", () -> HudLayout.openEditor(ClientCompat.screen()));

    public final BoolSetting light = bool("light", true).section("light").serverRule(InfoRules.LIGHT);
    public final BoolSetting lightTorch = bool("lightTorch", true).section("light").serverRule(InfoRules.LIGHT)
            .visibleWhen(this::lightOn);
    public final BoolSetting chunks = bool("chunks", true).section("light").serverRule(InfoRules.CHUNKS);

    public final BoolSetting card = bool("card", true).section("card").serverRule(InfoRules.CARD);
    public final BoolSetting cardMobs = bool("cardMobs", true).section("card").serverRule(InfoRules.CARD)
            .visibleWhen(this::cardOn);
    public final BoolSetting cardTool = bool("cardTool", true).section("card").serverRule(InfoRules.CARD)
            .visibleWhen(this::cardOn);
    public final BoolSetting cardMod = bool("cardMod", true).section("card").serverRule(InfoRules.CARD)
            .visibleWhen(this::cardOn);

    public final BoolSetting food = bool("food", true).section("food").serverRule(InfoRules.FOOD);
    public final BoolSetting saturation = bool("saturation", false).section("food").serverRule(InfoRules.SATURATION)
            .visibleWhen(this::foodOn);

    public final BoolSetting toolWarn = bool("toolWarn", true).section("tool").serverRule(InfoRules.TOOL);
    public final IntSetting toolWarnAt = intRange("toolWarnAt", 10, 1, 50).unit("%").section("tool")
            .serverRule(InfoRules.TOOL).visibleWhen(this::toolOn);

    public final BoolSetting tnt = bool("tnt", true).section("tnt").serverRule(InfoRules.TNT);

    public final KeybindSetting pinKey = keybind("pinKey", Keys.I).serverRule(InfoRules.CARD);
    public final KeybindSetting lightKey = keybind("lightKey", Keys.UNBOUND).serverRule(InfoRules.LIGHT)
            .onPress(() -> InfoTick.toggleLight(this));
    public final KeybindSetting chunkKey = keybind("chunkKey", Keys.O).serverRule(InfoRules.CHUNKS)
            .onPress(() -> ChunkBorders.toggle(this));

    public InfoModule() {
        super("info", ModuleCategory.MISC);
        instance = this;
    }

    public static InfoModule get() {
        return instance;
    }

    @Override
    public int accentColor() {
        return 0xFFFFE14D;
    }

    @Override
    public List<String> serverRuleKeys() {
        return InfoRules.ALL;
    }

    @Override
    public void onLoad() {
        for (String key : InfoRules.ALL) ServerRules.supports(key);
        ServerRules.listen(() -> InfoTick.rulesChanged(this));
        CardHud.register(this);
        ToolWarnHud.register(this);
    }

    @Override
    public void onDisable() {
        InfoTick.clear();
    }

    @Override
    public void onWorldLeave() {
        InfoTick.clear();
    }

    @Override
    public void tick() {
        InfoTick.tick(this);
    }

    @Override
    public void renderHud(Gfx g) {
        HungerBarOverlay.draw(g, this);
    }

    private boolean lightOn() {
        return light.on();
    }

    private boolean cardOn() {
        return card.on();
    }

    private boolean foodOn() {
        return food.on();
    }

    private boolean toolOn() {
        return toolWarn.on();
    }

    public boolean active(BoolSetting s, String rule) {
        return isEnabled() && s.on() && !ServerRules.isDisabled(rule);
    }

    public boolean lightActive() {
        return active(light, InfoRules.LIGHT);
    }

    public boolean chunksActive() {
        return active(chunks, InfoRules.CHUNKS);
    }

    public boolean cardActive() {
        return active(card, InfoRules.CARD);
    }

    public boolean foodActive() {
        return active(food, InfoRules.FOOD);
    }

    public boolean saturationActive() {
        return foodActive() && active(saturation, InfoRules.SATURATION);
    }

    public boolean toolActive() {
        return active(toolWarn, InfoRules.TOOL);
    }

    public boolean tntActive() {
        return active(tnt, InfoRules.TNT);
    }
}
