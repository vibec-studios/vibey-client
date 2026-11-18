package dev.henny.vibeyclient.api;

import dev.henny.vibeyclient.ModConflicts;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.ColorSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Panel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Base class of every Vibey Client module (client side only). Lifecycle, all on the client thread:
 * <ol>
 *   <li>constructor / field initialisers: declare settings and keybinds with the helpers below</li>
 *   <li>{@link #onLoad()}: once at startup, only if the module can run at all (no conflict, available, deps there).
 *       Register services, net channels, event listeners. Do not touch {@code Minecraft.getInstance()} state yet.</li>
 *   <li>{@link #onEnable()} / {@link #onDisable()}: whenever the player (or a dependency) toggles it; the first
 *       enable happens on the first client tick. Must be reversible without a restart.</li>
 *   <li>{@link #tick()}, {@link #renderHud(Gfx)}, {@link #onWorldJoin()}, {@link #onWorldLeave()}: only while enabled.</li>
 * </ol>
 * See docs/MODULES.md.
 */
public abstract class VibeyModule {
    private final String id;
    private final ModuleCategory category;
    private final SettingsGroup settings;
    protected final Logger log;
    boolean enabled;
    ModuleManager.Status status = ModuleManager.Status.DISABLED;
    String statusReasonKey;
    String statusReasonArg;
    int errors;

    protected VibeyModule(String id, ModuleCategory category) {
        if (!id.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Bad module id " + id);
        this.id = id;
        this.category = category;
        this.settings = new SettingsGroup(id);
        this.log = LoggerFactory.getLogger("VibeyClient/" + id);
    }

    // ============================================================================================ identity

    public final String id() {
        return id;
    }

    public final ModuleCategory category() {
        return category;
    }

    /** {@code <id>.name} in {@code i18n/<lang>/<id>.json} ({@code "name"} in the file). */
    public String nameKey() {
        return id + ".name";
    }

    /** {@code <id>.description}: one line shown under the name. */
    public String descriptionKey() {
        return id + ".description";
    }

    public String displayName() {
        return Lang.tr(nameKey());
    }

    /** Optional square icon texture id ("namespace:path/to.png"); null = a monogram tile in {@link #accentColor()}. */
    public String icon() {
        return null;
    }

    /** ARGB accent for the icon tile. */
    public int accentColor() {
        return 0xFF8B6CFF;
    }

    // ============================================================================================ rules

    /** Module ids that must be enabled for this one to run. Disabling one of them disables this module too. */
    public List<String> dependencies() {
        return List.of();
    }

    /** Mod ids that replace this module (installed → module auto-disabled, its mixins skipped). */
    public List<String> conflictingMods() {
        return ModConflicts.of(id);
    }

    public boolean defaultEnabled() {
        return true;
    }

    /** False if the module cannot run here at all (e.g. wrong loader). Checked once at startup. */
    public boolean available() {
        return true;
    }

    /** Shown in the menu? (e.g. social is hidden while online features are off) */
    public boolean visible() {
        return true;
    }

    /**
     * Extra lines shown on the module's settings page under its description (translated), e.g. "Lyrics are provided
     * by the installed Vibey Lyrics" when only a part of the module is off. Checked every frame, keep it cheap.
     */
    public List<String> notices() {
        return List.of();
    }

    /** Can the player switch it off? */
    public boolean canToggle() {
        return true;
    }

    // ============================================================================================ lifecycle

    public void onLoad() {}

    public void onEnable() {}

    public void onDisable() {}

    /** Every client tick (20/s, also on the title screen) while enabled. */
    public void tick() {}

    /** HUD overlay, every frame while enabled and the HUD is visible. Keep it allocation free. */
    public void renderHud(Gfx g) {}

    public void onWorldJoin() {}

    public void onWorldLeave() {}

    /** Custom settings page; null = the settings are rendered automatically. */
    public Panel createSettingsPanel() {
        return null;
    }

    // ============================================================================================ state

    public final boolean isEnabled() {
        return enabled;
    }

    public final ModuleManager.Status status() {
        return status;
    }

    /** Why the module is not running (translated), or null. */
    public final String statusReason() {
        return statusReasonKey == null ? null : Lang.tr(statusReasonKey, statusReasonArg == null ? "" : statusReasonArg);
    }

    public final SettingsGroup settings() {
        return settings;
    }

    /** Translated {@code <id>.<key>}. */
    public final String tr(String key, Object... args) {
        return Lang.tr(id + "." + key, args);
    }

    /** Listens to an event only while the module is enabled (subscribe once, e.g. in onLoad). */
    protected final <E> void on(Class<E> type, Consumer<? super E> listener) {
        Events.subscribe(type, e -> {
            if (enabled) listener.accept(e);
        });
    }

    // ============================================================================================ settings

    protected final BoolSetting bool(String key, boolean def) {
        return settings.add(new BoolSetting(key, def));
    }

    protected final IntSetting intRange(String key, int def, int min, int max) {
        return settings.add(new IntSetting(key, def, min, max, 1));
    }

    protected final IntSetting intRange(String key, int def, int min, int max, int step) {
        return settings.add(new IntSetting(key, def, min, max, step));
    }

    protected final FloatSetting floatRange(String key, float def, float min, float max, float step) {
        return settings.add(new FloatSetting(key, def, min, max, step));
    }

    protected final <E extends Enum<E>> EnumSetting<E> choice(String key, E def) {
        return settings.add(new EnumSetting<>(key, def));
    }

    protected final StringSetting string(String key, String def) {
        return settings.add(new StringSetting(key, def, 256));
    }

    protected final StringSetting string(String key, String def, int maxLength) {
        return settings.add(new StringSetting(key, def, maxLength));
    }

    protected final ColorSetting color(String key, int argb) {
        return settings.add(new ColorSetting(key, argb));
    }

    /** {@code defaultKey}: a GLFW key code ({@code Keys.K} from client/Keys) or {@code Keys.UNBOUND}. */
    protected final KeybindSetting keybind(String key, int defaultKey) {
        KeybindSetting k = settings.add(new KeybindSetting(id, key, defaultKey));
        k.setActive(() -> enabled);
        return k;
    }

    protected final ActionSetting action(String key, Runnable action) {
        return settings.add(new ActionSetting(key, action));
    }
}
