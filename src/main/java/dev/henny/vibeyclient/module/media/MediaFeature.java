package dev.henny.vibeyclient.module.media;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.ModConflicts;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.ColorSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * One part of the media module (lyrics + skies, in-world video screens, ...). A feature has its own on/off switch
 * inside Media, its own i18n file and can be replaced by a standalone mod on its own (then only this part is off,
 * the rest of Media keeps running). See docs/MEDIA.md.
 *
 * <pre>{@code
 * public final class VideoFeature extends MediaFeature {
 *     public final IntSetting volume = intRange("volume", 80, 0, 100, 5).unit("%");      // key "video.volume"
 *     public final HudElement overlay = hudElement(HudElement.builder("overlay")       // id "media.video.overlay"
 *             .size(160, 90).defaults(Anchor.TOP_RIGHT, -6, 30).render(this::drawOverlay));
 *
 *     public VideoFeature(MediaModule media) { super(media, "video"); }
 *
 *     @Override public void onEnable()  { ... }
 *     @Override public void renderHud(Gfx g) { HudLayout.draw(g, overlay); }
 * }
 * // module/media/MediaFeatures.java:  m.addFeature(new VideoFeature(m));
 * }</pre>
 *
 * <p>Lifecycle (client thread), like {@code VibeyModule}: constructor/field initialisers declare settings, keybinds and
 * HUD elements; {@link #onLoad()} once at startup if the feature can run; {@link #onEnable()}/{@link #onDisable()}
 * whenever Media or the feature's own switch changes (reversible without a restart); {@link #tick()},
 * {@link #renderHud(Gfx)}, {@link #onWorldJoin()}, {@link #onWorldLeave()} only while enabled. Exceptions in hooks
 * are caught and logged (first 3).
 *
 * <p>Strings: {@code assets/vibeyclient/i18n/<lang>/media.<id>.json} (keys prefixed {@code media.<id>.}): {@code name},
 * {@code description}, {@code setting.<key>} (+ {@code .desc}, enum values {@code setting.<key>.<value>}). Keybind names
 * go into the vanilla lang files as {@code key.vibeyclient.media.<id>.<key>}. Settings are stored in
 * {@code config/vibeyclient/media.json} as {@code "<id>.<key>"}; free-form data under {@code _data.<id>}.
 */
public abstract class MediaFeature {
    private final MediaModule media;
    private final String id;
    protected final Logger log;
    /** The feature's own switch, shown first among its settings ({@code media.<id>.setting.enabled}). */
    public final BoolSetting enabledSetting;
    boolean enabled;
    boolean loaded;
    String conflict;
    boolean unavailable;
    int errors;

    protected MediaFeature(MediaModule media, String id) {
        if (!id.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Bad media feature id " + id);
        this.media = media;
        this.id = id;
        this.log = LoggerFactory.getLogger("VibeyClient/media/" + id);
        for (String mod : conflictingMods()) if (Vibey.platform().isModLoaded(mod)) conflict = mod;
        this.enabledSetting = media.settings().add(new BoolSetting(id + ".enabled", defaultEnabled()))
                .labelKey(prefix() + "setting.enabled").visibleWhen(this::usable).section(id).sectionLabel(prefix() + "name");
    }

    // ============================================================================================ identity

    public final String id() {
        return id;
    }

    public final MediaModule media() {
        return media;
    }

    /** {@code media.<id>.} */
    public final String prefix() {
        return MediaModule.ID + "." + id + ".";
    }

    public String displayName() {
        return Lang.tr(prefix() + "name");
    }

    // ============================================================================================ rules

    /** Mods that replace only this part of Media; default {@code ModConflicts.of("media.<id>")}. Called from the
     *  constructor, so it must not use fields of the subclass. */
    public List<String> conflictingMods() {
        return ModConflicts.of(MediaModule.ID + "." + id);
    }

    /** False if the feature cannot run here at all. Checked once at startup. */
    public boolean available() {
        return true;
    }

    /** Default of the feature's own switch. Called from the constructor. */
    public boolean defaultEnabled() {
        return true;
    }

    // ============================================================================================ lifecycle

    public void onLoad() {}

    public void onEnable() {}

    public void onDisable() {}

    public void tick() {}

    /** Every frame while enabled and the HUD is visible. Keep it allocation free; draw HUD elements through
     *  {@link HudLayout#draw(Gfx, HudElement)} so the HUD editor positions apply. */
    public void renderHud(Gfx g) {}

    public void onWorldJoin() {}

    public void onWorldLeave() {}

    // ============================================================================================ state

    /** Media is on, the feature's switch is on and it can run (no conflict, available). */
    public final boolean isEnabled() {
        return enabled;
    }

    /** Can run at all: no standalone mod replaces it and it is available. */
    public final boolean usable() {
        return conflict == null && !unavailable;
    }

    /** The installed mod that replaces this feature, or null. */
    public final String conflict() {
        return conflict;
    }

    /** Translated {@code media.<id>.<key>}. */
    public final String tr(String key, Object... args) {
        return Lang.tr(prefix() + key, args);
    }

    /** Free-form persistent JSON of this feature ({@code _data.<id>} in media.json); call {@link #markDirty()} after a change. */
    public final JsonObject data() {
        JsonObject all = media.settings().data();
        synchronized (media.settings()) {
            if (!all.has(id) || !all.get(id).isJsonObject()) all.add(id, new JsonObject());
            return all.getAsJsonObject(id);
        }
    }

    public final void markDirty() {
        media.settings().markDirty();
    }

    /** Listens to an event only while the feature is enabled (subscribe once, e.g. in onLoad). */
    protected final <E> void on(Class<E> type, Consumer<? super E> listener) {
        Events.subscribe(type, e -> {
            if (enabled) listener.accept(e);
        });
    }

    // ============================================================================================ declarations

    private <S extends Setting<?, ?>> S add(S s) {
        media.settings().add(s);
        s.labelKey(prefix() + "setting." + s.key().substring(id.length() + 1));
        // one card per feature on the Media page, titled with the feature's name
        if (s.section() == null) s.section(id).sectionLabel(prefix() + "name");
        return s;
    }

    private String key(String key) {
        return id + "." + key;
    }

    protected final BoolSetting bool(String key, boolean def) {
        return add(new BoolSetting(key(key), def)).visibleWhen(this::usable);
    }

    protected final IntSetting intRange(String key, int def, int min, int max, int step) {
        return add(new IntSetting(key(key), def, min, max, step)).visibleWhen(this::usable);
    }

    protected final FloatSetting floatRange(String key, float def, float min, float max, float step) {
        return add(new FloatSetting(key(key), def, min, max, step)).visibleWhen(this::usable);
    }

    protected final <E extends Enum<E>> EnumSetting<E> choice(String key, E def) {
        return add(new EnumSetting<>(key(key), def)).visibleWhen(this::usable);
    }

    protected final StringSetting string(String key, String def, int maxLength) {
        return add(new StringSetting(key(key), def, maxLength)).visibleWhen(this::usable);
    }

    protected final ColorSetting color(String key, int argb) {
        return add(new ColorSetting(key(key), argb)).visibleWhen(this::usable);
    }

    protected final ActionSetting action(String key, Runnable action) {
        return add(new ActionSetting(key(key), action)).visibleWhen(this::usable);
    }

    /**
     * {@code key.vibeyclient.media.<id>.<key>}; only fires while the feature is enabled, and is not registered at all
     * (not in the controls screen) when a standalone mod replaces the feature. Constructor/field initialisers only.
     */
    protected final KeybindSetting keybind(String key, int defaultKey) {
        KeybindSetting k = add(new KeybindSetting(MediaModule.ID, key(key), defaultKey));
        k.setActive(() -> enabled);
        k.visibleWhen(this::usable);
        if (!usable()) k.skipRegistration();
        return k;
    }

    /**
     * A 2D element the player can move and scale in the HUD editor; its id becomes {@code media.<featureId>.<name>},
     * its label {@code media.<featureId>.hud.<name>} unless the builder sets one. Only shown in the editor while the
     * feature is enabled (and the builder's own condition, if any).
     */
    protected final HudElement hudElement(HudElement.Builder builder) {
        HudElement e = builder.prefix(MediaModule.ID + "." + id + ".").defaultLabel(prefix() + "hud.").andVisible(() -> enabled).build();
        HudLayout.register(e);
        return e;
    }

    // ============================================================================================ host side

    final void safely(String what, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            if (errors++ < 3) log.error("Media feature {} {} failed", id, what, t);
        }
    }
}
