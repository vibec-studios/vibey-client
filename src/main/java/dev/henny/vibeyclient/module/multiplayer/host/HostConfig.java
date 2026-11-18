package dev.henny.vibeyclient.module.multiplayer.host;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.multiplayer.MultiplayerModule;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings of the host. Lives in the module's settings file {@code config/vibeyclient/multiplayer.json}: the simple
 * values are the module's typed settings (shown in the Vibey menu, {@link MultiplayerModule}), the lists are stored
 * under {@code _data.host}. The host screens edit the fields here and call {@link #save()}, which pushes the values
 * into the typed settings; a change in the Vibey menu is copied back into the fields by the module.
 * Fields are public on purpose: Gson writes them 1:1 (same shape as the standalone mod's {@code config/vibey.json},
 * which is imported once on first start).
 */
public final class HostConfig {
    private static final Gson GSON = new GsonBuilder().create();
    private static HostConfig current = new HostConfig();

    // Simple
    public String gameMode = "survival";
    public boolean cheats = false;
    public boolean forceMods = false;

    // Access (enforced by the relay before anyone reaches the world, and again on join)
    public boolean whitelistEnabled = false;
    public List<String> whitelist = new ArrayList<>();
    public List<String> bannedNames = new ArrayList<>();
    /** Banned IP -> name of the player it belonged to (only for display). */
    public Map<String, String> bannedIps = new LinkedHashMap<>();

    // Advanced
    public boolean pvp = true;
    public int maxPlayers = 8;
    public boolean exactModVersions = false;
    public String relay = BuildInfo.DEFAULT_RELAY;
    public int lanPort = 0;
    public boolean advancedOpen = false;

    /** Last address the relay gave us, so the screen can show it before going online. */
    public String lastAddress = "";

    public static HostConfig get() {
        return current;
    }

    /** Startup (module onLoad, after its settings file was read). */
    public static void load(MultiplayerModule module) {
        JsonElement stored = module.settings().data().get("host");
        boolean imported = false;
        if (stored != null && stored.isJsonObject()) {
            try {
                HostConfig c = GSON.fromJson(stored, HostConfig.class);
                if (c != null) current = c;
            } catch (Exception e) {
                MultiplayerModule.LOG.warn("Bad host data in multiplayer.json, using defaults: {}", e.toString());
            }
            module.copySettingsInto(current);
        } else {
            // First start: take over the standalone Vibey Multiplayer settings if there are any.
            Path legacy = Vibey.platform().configDir().resolve("vibey.json");
            if (Files.isRegularFile(legacy)) {
                try {
                    HostConfig c = GSON.fromJson(Files.readString(legacy, StandardCharsets.UTF_8), HostConfig.class);
                    if (c != null) {
                        current = c;
                        imported = true;
                        MultiplayerModule.LOG.info("Imported the host settings of Vibey Multiplayer from {}", legacy);
                    }
                } catch (Exception e) {
                    MultiplayerModule.LOG.warn("Could not import {}: {}", legacy, e.toString());
                }
            }
            if (!imported) module.copySettingsInto(current);
        }
        current.sanitize();
        if (imported || stored == null) save();
    }

    /** Pushes the fields into the module settings and schedules a write of the settings file. */
    public static void save() {
        MultiplayerModule module = MultiplayerModule.instance();
        if (module == null) return;
        current.sanitize();
        module.copySettingsFrom(current);
        synchronized (module.settings()) {
            module.settings().data().add("host", GSON.toJsonTree(current));
        }
        module.settings().markDirty();
    }

    private void sanitize() {
        if (relay == null || relay.isBlank()) relay = BuildInfo.DEFAULT_RELAY;
        relay = relay.trim();
        if (maxPlayers < 2) maxPlayers = 2;
        if (maxPlayers > 8) maxPlayers = 8;
        if (lanPort < 0 || lanPort > 65535) lanPort = 0;
        if (gameMode == null) gameMode = "survival";
        if (lastAddress == null) lastAddress = "";
        if (whitelist == null) whitelist = new ArrayList<>();
        if (bannedNames == null) bannedNames = new ArrayList<>();
        if (bannedIps == null) bannedIps = new LinkedHashMap<>();
    }
}
