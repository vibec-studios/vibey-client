package dev.henny.vibeyclient.module.social;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;

/** Minecraft calls of the social module whose shape changed between 1.21.1 and 26.3. */
public final class SocialCompat {
    private SocialCompat() {}

    public static boolean windowActive() {
        return Minecraft.getInstance().isWindowActive();
    }

    /** Name of the open singleplayer world, or null. */
    public static String singleplayerWorld() {
        IntegratedServer s = Minecraft.getInstance().getSingleplayerServer();
        if (s == null) return null;
        try {
            return s.getWorldData().getLevelName();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static boolean worldOpened() {
        IntegratedServer s = Minecraft.getInstance().getSingleplayerServer();
        return s != null && s.isPublished();
    }

    /** Address of the multiplayer server we are on, or null (singleplayer / menus). */
    public static String serverAddress() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getSingleplayerServer() != null) return null;
        ServerData d = mc.getCurrentServer();
        return d == null ? null : d.ip;
    }

    public static String serverListName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getSingleplayerServer() != null) return null;
        ServerData d = mc.getCurrentServer();
        if (d == null || d.name == null || d.name.isBlank()) return null;
        String n = d.name.trim();
        if (n.equalsIgnoreCase(d.ip == null ? "" : d.ip.trim())) return null;
        if (n.equals(net.minecraft.client.resources.language.I18n.get("selectServer.defaultName"))) return null;
        return n;
    }

    public static boolean inWorld() {
        return Minecraft.getInstance().level != null;
    }

    /** Leaves the current world/server like the pause menu's "Save and quit" / "Disconnect". Client thread. */
    public static void leaveWorld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        //? if >=1.21.9 {
        mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
        //?} else if >=1.21.6 {
        /*net.minecraft.client.gui.screens.PauseScreen.disconnectFromWorld(mc, net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
        *///?} else {
        /*boolean local = mc.isLocalServer();
        mc.level.disconnect();
        if (local) mc.disconnect(new net.minecraft.client.gui.screens.GenericMessageScreen(Component.translatable("menu.savingLevel")));
        else mc.disconnect();
        *///?}
    }

    /** Opens the vanilla connect flow to {@code address:port}. Client thread, not in a world. */
    public static void connect(String name, String address, int port) {
        Minecraft mc = Minecraft.getInstance();
        String full = port <= 0 || port == 25565 ? address : address + ":" + port;
        ServerData data = new ServerData(name == null ? full : name, full, ServerData.Type.OTHER);
        ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(full), data, false, null);
    }

    // -------------------------------------------------------------------------------------------- screens behind ours

    //? if >=26.1 {
    /*public static void renderBehind(Screen parent, GuiGraphicsExtractor g, float delta) {
        parent.extractBackground(g, -1, -1, delta);
        parent.extractRenderState(g, -1, -1, delta);
    }
    *///?} else {
    public static void renderBehind(Screen parent, GuiGraphics g, float delta) {
        //? if >=1.21.6
        parent.renderBackground(g, -1, -1, delta);
        parent.render(g, -1, -1, delta);
    }
    //?}

    public static void resize(Screen parent, int width, int height) {
        //? if >=1.21.11 {
        parent.resize(width, height);
        //?} else
        /*parent.resize(Minecraft.getInstance(), width, height);*/
    }

    public static Component literal(String s) {
        return Component.literal(s);
    }
}
