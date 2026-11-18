package dev.henny.vibeyclient.module.media;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.media.lyrics.LyricsFeature;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.MenuEntries;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Media: music, lyrics and skies ({@link LyricsFeature}, ported from Vibey Lyrics), in-world video screens and
 * whatever else plays something. The module itself is a small host: it owns the settings file, the HUD editor entry
 * and the title/pause menu entry, and runs its {@link MediaFeature}s (each with its own switch, i18n file and
 * standalone-mod conflict). Fabric only (not compiled for NeoForge). Was the module "lyrics" before 0.3: its
 * settings, on/off state and keybinds are carried over on the first start ({@link MediaMigration}).
 * See docs/MEDIA.md.
 */
public final class MediaModule extends VibeyModule {
    public static final String ID = "media";
    private static MediaModule instance;

    public final ActionSetting editHud = action("editHud", () -> HudLayout.openEditor(ClientCompat.screen()));
    public final KeybindSetting hudEditor = keybind("hudEditor", Keys.UNBOUND).onPress(() -> {
        if (ClientCompat.screen() == null) HudLayout.openEditor(null);
    });

    private final List<MediaFeature> features = new ArrayList<>();
    private boolean loaded;

    public MediaModule() {
        super(ID, ModuleCategory.MEDIA);
        instance = this;
        MediaMigration.run();
        MediaFeatures.registerAll(this);
    }

    public static MediaModule instance() {
        return instance;
    }

    // ============================================================================================ features

    /**
     * Adds a feature. Call it from {@link MediaFeatures#registerAll} (one line per feature), i.e. while the game
     * starts: keybinds can only be declared then. Settings added later still get their stored values.
     */
    public <F extends MediaFeature> F addFeature(F feature) {
        if (feature.media() != this) throw new IllegalArgumentException("Feature of another media module");
        for (MediaFeature f : features) {
            if (f.id().equals(feature.id())) throw new IllegalArgumentException("Duplicate media feature " + feature.id());
        }
        features.add(feature);
        feature.enabledSetting.onChange(on -> sync(feature));
        if (loaded) load(feature);
        return feature;
    }

    public List<MediaFeature> features() {
        return Collections.unmodifiableList(features);
    }

    /** The feature of this type, or null. */
    public <F extends MediaFeature> F feature(Class<F> type) {
        for (MediaFeature f : features) if (type.isInstance(f)) return type.cast(f);
        return null;
    }

    public MediaFeature feature(String id) {
        for (MediaFeature f : features) if (f.id().equals(id)) return f;
        return null;
    }

    /** Media is on and the feature of this type runs. */
    public static boolean running(Class<? extends MediaFeature> type) {
        MediaModule m = instance;
        if (m == null || !m.isEnabled()) return false;
        MediaFeature f = m.feature(type);
        return f != null && f.isEnabled();
    }

    // ============================================================================================ module

    /** Fabric only, like the standalone Vibey Lyrics (this package is not even compiled for NeoForge). */
    @Override
    public boolean available() {
        return Vibey.platform().isFabric();
    }

    @Override
    public int accentColor() {
        return 0xFFFF5470;
    }

    /** Parts that a standalone mod provides, shown on the settings page. */
    @Override
    public List<String> notices() {
        List<String> out = new ArrayList<>();
        for (MediaFeature f : features) {
            if (f.conflict() != null) out.add(Lang.tr("media.notice.conflict", f.displayName(), modName(f.conflict())));
        }
        return out;
    }

    @Override
    public void onLoad() {
        loaded = true;
        for (MediaFeature f : features) load(f);
        MenuEntries.Entry entry = new MenuEntries.Entry("media", "media.menu", Icons.NOTE, 35,
                MenuEntries.whenEnabled(ID), MediaModule::openMain);
        MenuEntries.register(MenuEntries.Place.TITLE, entry);
        MenuEntries.register(MenuEntries.Place.PAUSE, new MenuEntries.Entry("media", "media.menu", Icons.NOTE, 40,
                MenuEntries.whenEnabled(ID), MediaModule::openMain));
    }

    private void load(MediaFeature f) {
        Lang.addFile(ID + "." + f.id());
        if (f.conflict() != null) {
            log.info("Media part '{}' is off: provided by the installed mod '{}'", f.id(), f.conflict());
            return;
        }
        try {
            f.unavailable = !f.available();
        } catch (Throwable t) {
            f.unavailable = true;
        }
        if (f.unavailable) return;
        f.safely("load", f::onLoad);
        f.loaded = true;
        if (isEnabled()) sync(f);
    }

    @Override
    public void onEnable() {
        for (MediaFeature f : features) sync(f);
    }

    @Override
    public void onDisable() {
        for (int i = features.size() - 1; i >= 0; i--) sync(features.get(i));
    }

    /** Brings one feature in line with Media's state and its own switch. */
    private void sync(MediaFeature f) {
        boolean want = isEnabled() && f.loaded && f.usable() && f.enabledSetting.get();
        if (want == f.enabled) return;
        f.enabled = want;
        f.safely(want ? "enable" : "disable", want ? f::onEnable : f::onDisable);
    }

    @Override
    public void tick() {
        for (MediaFeature f : features) if (f.enabled) f.safely("tick", f::tick);
    }

    @Override
    public void renderHud(Gfx g) {
        for (MediaFeature f : features) if (f.enabled) f.safely("hud", () -> f.renderHud(g));
    }

    @Override
    public void onWorldJoin() {
        for (MediaFeature f : features) if (f.enabled) f.safely("world join", f::onWorldJoin);
    }

    @Override
    public void onWorldLeave() {
        for (MediaFeature f : features) if (f.enabled) f.safely("world leave", f::onWorldLeave);
    }

    /** The "Media" entry of the title screen and pause menu: the music player, else Media's settings page. */
    public static void openMain(Screen parent) {
        LyricsFeature ly = instance == null ? null : instance.feature(LyricsFeature.class);
        if (ly != null && ly.isEnabled()) {
            ly.open(parent);
            return;
        }
        ModulesPanel.preselect(ID);
        ClientCompat.openScreen(new VibeyScreen(parent));
    }

    private static String modName(String id) {
        for (var info : Vibey.platform().mods()) if (info.id().equals(id)) return info.name();
        return id;
    }

    /** Status of every feature for logs and tests: "lyrics=on video=off(conflict)". */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        for (MediaFeature f : features) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(f.id()).append('=').append(f.enabled ? "on" : "off");
            if (f.conflict() != null) sb.append("(conflict)");
            else if (f.unavailable) sb.append("(unavailable)");
        }
        return sb.toString();
    }

    static boolean moduleEnabled() {
        return ModuleManager.get().isEnabled(ID);
    }
}
