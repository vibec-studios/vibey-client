package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.module.servers.ServersModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;

import java.util.Map;
import java.util.WeakHashMap;

public final class ServerSessions {
    private static final Map<JoinMultiplayerScreen, ServerSession> OPEN = new WeakHashMap<>();

    private ServerSessions() {}

    static ServerSession open(ServersModule m, JoinMultiplayerScreen screen) {
        return OPEN.computeIfAbsent(screen, s -> new ServerSession(m, s));
    }

    static void close(JoinMultiplayerScreen screen) {
        ServerSession s = OPEN.remove(screen);
        if (s != null) s.close();
    }

    public static ServerSession current() {
        return Minecraft.getInstance().screen instanceof JoinMultiplayerScreen s ? OPEN.get(s) : null;
    }

    public static boolean typed(int codepoint) {
        ServerSession s = current();
        return s != null && s.typed(codepoint);
    }
}
