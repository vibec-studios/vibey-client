package dev.henny.vibeyclient.module.media.video.web;

import dev.henny.vibeyclient.module.media.video.MediaSource;
import dev.henny.vibeyclient.module.media.video.player.MediaPlayer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The browser (YouTube, web pages) through the optional MCEF Modern mod. This facade never mentions MCEF types,
 * so nothing of MCEF is loaded unless the mod is installed; {@link McefBridge} does the real work.
 */
public final class Web {
    public static final String MOD_ID = "mcef-modern";
    public static final String MOD_NAME = "MCEF Modern";
    public static final String MOD_LICENSE = "LGPL-2.1";
    public static final String MOD_SOURCE = "modrinth.com/mod/mcef-modern";
    /** What MCEF itself downloads on first start (jcefmaven natives from Maven Central). */
    public static final String NATIVES_SOURCE = "Maven Central (me.friwi:jcef-natives)";
    public static final int NATIVES_MB = 160;

    public enum State { NOT_INSTALLED, NEEDS_SETUP, STARTING, READY, FAILED }

    private Web() {}

    public static boolean installed() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    /** The Chromium files are already on disk (MCEF downloaded them before). */
    public static boolean nativesPresent() {
        Boolean c = nativesCache;
        if (c != null) return c;
        Path dir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("jcef");
        boolean present;
        try (var s = Files.list(dir)) {
            present = s.findAny().isPresent();
        } catch (Exception e) {
            present = false;
        }
        nativesCache = present;
        return present;
    }

    private static volatile Boolean nativesCache;

    public static State state(boolean consent) {
        if (!installed()) return State.NOT_INSTALLED;
        if (McefBridge.started()) return McefBridge.state();
        return consent || nativesPresent() ? State.STARTING : State.NEEDS_SETUP;
    }

    /** Starts MCEF (it downloads Chromium on the first start). Only call after the user agreed. */
    public static void start() {
        if (installed()) McefBridge.start();
    }

    /** 0..100 or -1 while MCEF downloads/unpacks Chromium. */
    public static float progress() {
        return installed() && McefBridge.started() ? McefBridge.percentage() : -1f;
    }

    /** "DOWNLOADING", "EXTRACTING", ... */
    public static String stage() {
        return installed() && McefBridge.started() ? McefBridge.stage() : "";
    }

    public static String failure() {
        return installed() ? McefBridge.failure() : null;
    }

    /** Opens the mouse/keyboard view of a web screen (no-op for other players). */
    public static void openBrowser(net.minecraft.client.gui.screens.Screen parent, MediaPlayer player) {
        if (installed() && player instanceof WebPlayer wp) net.minecraft.client.Minecraft.getInstance().setScreen(new BrowserScreen(parent, wp));
    }

    /** A browser player; only when {@link #state} is READY. */
    public static MediaPlayer create(MediaSource source, int width, int height, long startMs, boolean playing) {
        return McefBridge.create(source, width, height, startMs, playing);
    }
}
