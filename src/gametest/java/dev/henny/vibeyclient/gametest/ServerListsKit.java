package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.servers.screen.ServerSession;
import dev.henny.vibeyclient.module.servers.screen.ServerSessions;
import dev.henny.vui.dom.Element;
import dev.henny.vui.geom.Rect;
import dev.henny.vui.layout.ScreenRects;
import dev.henny.vui.mc.host.DocumentView;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.multiplayer.ServerList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

final class ServerListsKit {
    static final ReviewShots SHOTS = new ReviewShots("servers-friends");

    private ServerListsKit() {}

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static void log(String format, Object... args) {
        System.out.println("[vibeyclient-serverlists-test] " + String.format(Locale.ROOT, format, args));
    }

    static ServerSession session() {
        return ServerSessions.current();
    }

    static List<String> saved(ClientGameTestContext ctx, String prefix) {
        return ctx.computeOnClient(mc -> {
            ServerList list = new ServerList(mc);
            list.load();
            List<String> out = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) if (list.get(i).name.startsWith(prefix)) out.add(list.get(i).name);
            return out;
        });
    }

    static boolean savedAddress(ClientGameTestContext ctx, String address) {
        return ctx.computeOnClient(mc -> {
            ServerList list = new ServerList(mc);
            list.load();
            for (int i = 0; i < list.size(); i++) if (list.get(i).ip.equals(address)) return true;
            return false;
        });
    }

    static float[] box(ClientGameTestContext ctx, Function<ServerSession, DocumentView> view, String id) {
        return ctx.computeOnClient(mc -> {
            ServerSession s = session();
            if (s == null) return null;
            Element e = view.apply(s).document().find(id);
            if (e == null) {
                var list = view.apply(s).document().query(id);
                e = list.isEmpty() ? null : list.get(0);
            }
            if (e == null) return null;
            Rect r = ScreenRects.of(e);
            return new float[] {r.x(), r.y(), r.width(), r.height()};
        });
    }

    static void cursor(ClientGameTestContext ctx, double x, double y) {
        double gs = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
        ctx.getInput().setCursorPos(x * gs + gs / 2, y * gs + gs / 2);
        ctx.waitTicks(1);
    }

    static void click(ClientGameTestContext ctx, double x, double y) {
        cursor(ctx, x, y);
        ctx.waitTicks(1);
        ctx.getInput().pressMouse(0);
        ctx.waitTicks(3);
    }

    static void clickCenter(ClientGameTestContext ctx, float[] b) {
        check(b != null, "element missing");
        click(ctx, b[0] + b[2] / 2, b[1] + b[3] / 2);
    }

    static void glide(ClientGameTestContext ctx, double x0, double y0, double x1, double y1, int steps) {
        for (int i = 1; i <= steps; i++) cursor(ctx, x0 + (x1 - x0) * i / steps, y0 + (y1 - y0) * i / steps);
    }

    static void drag(ClientGameTestContext ctx, double x0, double y0, double x1, double y1, Runnable beforeRelease) {
        cursor(ctx, x0, y0);
        ctx.waitTicks(1);
        ctx.getInput().holdMouse(0);
        ctx.waitTicks(2);
        glide(ctx, x0, y0, x1, y1, 8);
        ctx.waitTicks(6);
        if (beforeRelease != null) beforeRelease.run();
        ctx.getInput().releaseMouse(0);
        ctx.waitTicks(4);
    }
}
