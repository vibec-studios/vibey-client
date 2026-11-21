package dev.henny.vibeyclient.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Platform;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.ui.Gfx;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Owns all modules: registration order, conflicts with standalone mods, dependencies, runtime enable/disable and
 * the persisted on/off state ({@code config/vibeyclient/modules.json}). Client thread only.
 */
public final class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();

    public enum Status {
        /** Running. */
        ENABLED,
        /** Switched off (by the player or because a dependency is off). */
        DISABLED,
        /** The standalone mod that provides this feature is installed. */
        CONFLICT,
        /** Cannot run here ({@link VibeyModule#available()} is false). */
        UNAVAILABLE,
        /** A dependency is missing or cannot run. */
        MISSING_DEPENDENCY;

        public boolean loadable() {
            return this == ENABLED || this == DISABLED;
        }
    }

    private final Map<String, VibeyModule> modules = new LinkedHashMap<>();
    private final Map<String, Boolean> wanted = new LinkedHashMap<>();
    private boolean loaded;
    private boolean started;
    private java.util.function.BooleanSupplier allowed = () -> true;

    private ModuleManager() {}

    public static ModuleManager get() {
        return INSTANCE;
    }

    // ============================================================================================ registry

    /** Registers a module. Only before startup ({@code VibeyCore.init}); the order is the menu order. */
    public <M extends VibeyModule> M register(M module) {
        if (loaded) throw new IllegalStateException("Modules must be registered before startup: " + module.id());
        if (modules.containsKey(module.id())) throw new IllegalArgumentException("Duplicate module " + module.id());
        modules.put(module.id(), module);
        return module;
    }

    public List<VibeyModule> all() {
        return Collections.unmodifiableList(new ArrayList<>(modules.values()));
    }

    public VibeyModule get(String id) {
        return modules.get(id);
    }

    public <M extends VibeyModule> M get(Class<M> type) {
        for (VibeyModule m : modules.values()) if (type.isInstance(m)) return type.cast(m);
        return null;
    }

    public boolean isEnabled(String id) {
        VibeyModule m = modules.get(id);
        return m != null && m.enabled;
    }

    // ============================================================================================ startup

    /** Startup: conflicts, availability, dependencies, settings, onLoad. Called once by the core. */
    public void load() {
        if (loaded) return;
        loaded = true;
        Platform p = Vibey.platform();
        for (VibeyModule m : modules.values()) {
            String conflict = null;
            for (String mod : m.conflictingMods()) if (p.isModLoaded(mod)) conflict = mod;
            if (conflict != null) {
                set(m, Status.CONFLICT, "core.module.conflict", modName(conflict));
                Vibey.LOG.info("Module {} disabled: provided by the installed mod '{}'", m.id(), conflict);
            } else if (!safeAvailable(m)) {
                set(m, Status.UNAVAILABLE, "core.module.unavailable", null);
            } else {
                set(m, Status.DISABLED, null, null);
            }
        }
        // Dependencies (repeat until stable: a missing dependency can cascade).
        boolean changed = true;
        while (changed) {
            changed = false;
            for (VibeyModule m : modules.values()) {
                if (!m.status.loadable()) continue;
                for (String dep : m.dependencies()) {
                    VibeyModule d = modules.get(dep);
                    if (d == null || !d.status.loadable()) {
                        set(m, Status.MISSING_DEPENDENCY, "core.module.missingDependency", d == null ? dep : d.id());
                        changed = true;
                        break;
                    }
                }
            }
        }
        readState();
        for (VibeyModule m : modules.values()) {
            if (!m.status.loadable()) continue;
            Lang.addFile(m.id());
            m.settings().load();
            try {
                m.onLoad();
            } catch (Throwable t) {
                Vibey.LOG.error("Module {} failed to load", m.id(), t);
                set(m, Status.UNAVAILABLE, "core.module.failed", null);
            }
        }
        // Conflicting modules keep their i18n so the menu can name them.
        for (VibeyModule m : modules.values()) Lang.addFile(m.id());
    }

    /** First client tick: enables what should run. */
    public void start() {
        if (started || !allowed.getAsBoolean()) return;
        started = true;
        for (VibeyModule m : modules.values()) {
            if (m.status.loadable() && wanted.getOrDefault(m.id(), m.defaultEnabled())) enable(m, false);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public void gate(java.util.function.BooleanSupplier allowed) {
        this.allowed = allowed;
    }

    public void suspend() {
        if (!started) return;
        started = false;
        List<VibeyModule> list = new ArrayList<>(modules.values());
        Collections.reverse(list);
        for (VibeyModule m : list) disable(m, false);
    }

    // ============================================================================================ toggling

    /** Switches a module on/off at runtime (dependencies follow). Returns false if it cannot change. */
    public boolean setEnabled(String id, boolean on) {
        VibeyModule m = modules.get(id);
        if (m == null || !m.status.loadable() || (!m.canToggle() && !on)) return false;
        if (on && !allowed.getAsBoolean()) return false;
        boolean ok = on ? enable(m, true) : disable(m, true);
        saveState();
        return ok;
    }

    public boolean toggle(String id) {
        return setEnabled(id, !isEnabled(id));
    }

    private boolean enable(VibeyModule m, boolean byPlayer) {
        if (m.enabled) return true;
        if (!m.status.loadable()) return false;
        for (String dep : m.dependencies()) {
            VibeyModule d = modules.get(dep);
            if (d == null || !enable(d, byPlayer)) return false;
        }
        m.enabled = true;
        set(m, Status.ENABLED, null, null);
        if (byPlayer) wanted.put(m.id(), true);
        try {
            m.onEnable();
        } catch (Throwable t) {
            Vibey.LOG.error("Module {} failed to enable", m.id(), t);
        }
        Events.post(new VibeyEvents.ModuleToggled(m.id(), true));
        return true;
    }

    private boolean disable(VibeyModule m, boolean byPlayer) {
        if (!m.enabled) return true;
        for (VibeyModule other : modules.values()) {
            if (other.enabled && other.dependencies().contains(m.id())) disable(other, byPlayer);
        }
        m.enabled = false;
        set(m, Status.DISABLED, null, null);
        if (byPlayer) wanted.put(m.id(), false);
        try {
            m.onDisable();
        } catch (Throwable t) {
            Vibey.LOG.error("Module {} failed to disable", m.id(), t);
        }
        Events.post(new VibeyEvents.ModuleToggled(m.id(), false));
        return true;
    }

    // ============================================================================================ hooks (core only)

    public void forEachEnabled(Consumer<VibeyModule> action) {
        for (VibeyModule m : modules.values()) {
            if (!m.enabled) continue;
            try {
                action.accept(m);
            } catch (Throwable t) {
                if (m.errors++ < 3) Vibey.LOG.error("Module {} threw", m.id(), t);
            }
        }
    }

    public void tick() {
        for (VibeyModule m : modules.values()) {
            if (!m.enabled) continue;
            try {
                m.tick();
            } catch (Throwable t) {
                if (m.errors++ < 3) Vibey.LOG.error("Module {} tick failed", m.id(), t);
            }
        }
    }

    public void renderHud(Gfx g) {
        for (VibeyModule m : modules.values()) {
            if (!m.enabled) continue;
            try {
                m.renderHud(g);
            } catch (Throwable t) {
                if (m.errors++ < 3) Vibey.LOG.error("Module {} HUD failed", m.id(), t);
            }
        }
    }

    // ============================================================================================ util

    private static void set(VibeyModule m, Status status, String reasonKey, String reasonArg) {
        m.status = status;
        m.statusReasonKey = reasonKey;
        m.statusReasonArg = reasonArg;
    }

    private static boolean safeAvailable(VibeyModule m) {
        try {
            return m.available();
        } catch (Throwable t) {
            Vibey.LOG.error("Module {} availability check failed", m.id(), t);
            return false;
        }
    }

    private static String modName(String id) {
        for (Platform.ModInfo info : Vibey.platform().mods()) if (info.id().equals(id)) return info.name();
        return id;
    }

    private Path stateFile() {
        return SettingsGroup.configDir().resolve("modules.json");
    }

    private void readState() {
        Path f = stateFile();
        if (!Files.isRegularFile(f)) return;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : json.keySet()) wanted.put(key, json.get(key).getAsBoolean());
        } catch (Exception e) {
            Vibey.LOG.warn("Could not read {}: {}", f, e.toString());
        }
    }

    private void saveState() {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Boolean> e : wanted.entrySet()) json.addProperty(e.getKey(), e.getValue());
        String text = new Gson().newBuilder().setPrettyPrinting().create().toJson(json);
        Path f = stateFile();
        Tasks.debounce(this, 300, () -> {
            try {
                Files.createDirectories(f.getParent());
                Files.writeString(f, text, StandardCharsets.UTF_8);
            } catch (Exception e) {
                Vibey.LOG.warn("Could not save {}: {}", f, e.toString());
            }
        });
    }
}
