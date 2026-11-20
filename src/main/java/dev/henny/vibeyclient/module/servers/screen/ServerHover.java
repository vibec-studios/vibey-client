package dev.henny.vibeyclient.module.servers.screen;

import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

public final class ServerHover {
    private static ServerSelectionList.Entry entry;
    private static long marked = -1;
    private static long frame;

    private ServerHover() {}

    public static void mark(ServerSelectionList.Entry e) {
        entry = e;
        marked = frame;
    }

    static ServerSelectionList.Entry take() {
        ServerSelectionList.Entry e = marked == frame ? entry : null;
        frame++;
        return e;
    }

    static void clear() {
        entry = null;
        marked = -1;
    }
}
