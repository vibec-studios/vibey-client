package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.servers.mixin.JoinMultiplayerAccess;
import dev.henny.vibeyclient.module.servers.screen.StarMark;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ServersTestKit {
    static final ReviewShots SHOTS = new ReviewShots("servers");

    private ServersTestKit() {}

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static void log(String format, Object... args) {
        System.out.println("[vibeyclient-servers-test] " + String.format(Locale.ROOT, format, args));
    }

    static void addServers(ClientGameTestContext ctx, String[] names, String[] addresses) {
        ctx.runOnClient(mc -> {
            ServerList list = new ServerList(mc);
            list.load();
            for (int i = 0; i < names.length; i++) list.add(new ServerData(names[i], addresses[i], ServerData.Type.OTHER), false);
            list.save();
        });
    }

    static void removeServers(ClientGameTestContext ctx, String... names) {
        ctx.runOnClient(mc -> {
            ServerList list = new ServerList(mc);
            list.load();
            for (String n : names)
                for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).name.equals(n)) list.remove(list.get(i));
            list.save();
        });
    }

    static ServerSelectionList list() {
        return Minecraft.getInstance().screen instanceof JoinMultiplayerScreen s ? ((JoinMultiplayerAccess) s).vibey$list() : null;
    }

    static ServerSelectionList.OnlineServerEntry entry(String name) {
        ServerSelectionList list = list();
        if (list == null) return null;
        for (ServerSelectionList.Entry e : list.children())
            if (e instanceof ServerSelectionList.OnlineServerEntry o && o.getServerData().name.equals(name)) return o;
        return null;
    }

    static ServerData data(String name) {
        ServerSelectionList.OnlineServerEntry e = entry(name);
        return e == null ? null : e.getServerData();
    }

    static List<String> online() {
        List<String> out = new ArrayList<>();
        ServerSelectionList list = list();
        if (list != null) for (ServerSelectionList.Entry e : list.children())
            if (e instanceof ServerSelectionList.OnlineServerEntry o) out.add(o.getServerData().name);
        return out;
    }

    static List<String> online(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> online());
    }

    static void hoverRow(ClientGameTestContext ctx, String name) {
        double[] p = ctx.computeOnClient(mc -> {
            ServerSelectionList.OnlineServerEntry e = entry(name);
            double gs = mc.getWindow().getGuiScale();
            return new double[] {(e.getContentX() + 120) * gs, (e.getContentY() + 16) * gs};
        });
        ctx.getInput().setCursorPos(p[0], p[1]);
        ctx.waitTicks(3);
    }

    static void clickStar(ClientGameTestContext ctx, String name) {
        hoverRow(ctx, name);
        double[] p = ctx.computeOnClient(mc -> {
            ServerSelectionList.OnlineServerEntry e = entry(name);
            double gs = mc.getWindow().getGuiScale();
            return new double[] {(StarMark.x(e) + 4) * gs, (StarMark.y(e) + 4) * gs};
        });
        ctx.getInput().setCursorPos(p[0], p[1]);
        ctx.waitTicks(3);
        ctx.getInput().pressMouse(0);
        ctx.waitTicks(4);
    }
}
