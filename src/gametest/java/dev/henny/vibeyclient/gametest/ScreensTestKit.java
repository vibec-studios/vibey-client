package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.module.screens.Screens;
import dev.henny.vibeyclient.module.screens.ScreensModule;
import dev.henny.vibeyclient.module.screens.apply.Item;
import dev.henny.vibeyclient.module.screens.apply.ScreenSession;
import dev.henny.vibeyclient.module.screens.apply.Sessions;
import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.Placement;
import dev.henny.vibeyclient.module.screens.core.ScreenLayout;
import dev.henny.vibeyclient.module.screens.edit.EditMode;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ScreensTestKit {
    static final ReviewKit R = new ReviewKit("screens");

    private ScreensTestKit() {}

    static ScreensModule m() {
        return ModuleManager.get().get(ScreensModule.class);
    }

    static void check(boolean ok, String message) {
        ReviewKit.check(ok, message);
    }

    static void log(String format, Object... args) {
        R.log(format, args);
    }

    static void shot(ClientGameTestContext ctx, String name) {
        R.shot(ctx, name);
    }

    static ScreenSession session(Minecraft mc) {
        return Sessions.peek(mc.screen);
    }

    static float[] box(ClientGameTestContext ctx, String id) {
        return ctx.computeOnClient(mc -> {
            ScreenSession s = session(mc);
            Item i = s == null ? null : s.item(id);
            if (i == null) return null;
            Box b = i.visual;
            return new float[] {b.x(), b.y(), b.w(), b.h()};
        });
    }

    static List<String> ids(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> {
            List<String> out = new ArrayList<>();
            ScreenSession s = session(mc);
            if (s == null) return out;
            if (s.panel() != null) out.add(s.panel().id);
            for (Item i : s.items()) out.add(i.id);
            return out;
        });
    }

    static String find(ClientGameTestContext ctx, String part) {
        for (String id : ids(ctx)) if (id.contains(part)) return id;
        throw new AssertionError("no widget like '" + part + "' in " + ids(ctx));
    }

    static ScreenLayout layout(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> session(mc).layout());
    }

    static Placement placement(ClientGameTestContext ctx, String id) {
        return layout(ctx).placement(id);
    }

    static boolean editing(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> EditMode.active(mc.screen));
    }

    static double[] window(ClientGameTestContext ctx, double vx, double vy) {
        return ctx.computeOnClient(mc -> {
            ScreenSession s = session(mc);
            double k = s == null ? 1 : s.scale();
            double gs = mc.getWindow().getGuiScale();
            return new double[] {vx * k * gs, vy * k * gs};
        });
    }

    static void hover(ClientGameTestContext ctx, double vx, double vy) {
        double[] p = window(ctx, vx, vy);
        ctx.getInput().setCursorPos(p[0], p[1]);
        ctx.waitTicks(2);
    }

    static void click(ClientGameTestContext ctx, double vx, double vy, int button) {
        hover(ctx, vx, vy);
        ctx.getInput().pressMouse(button);
        ctx.waitTicks(3);
    }

    static void drag(ClientGameTestContext ctx, double fx, double fy, double tx, double ty) {
        hover(ctx, fx, fy);
        ctx.getInput().holdMouse(0);
        ctx.waitTicks(2);
        for (int i = 1; i <= 8; i++) {
            hover(ctx, fx + (tx - fx) * i / 8, fy + (ty - fy) * i / 8);
        }
        ctx.getInput().releaseMouse(0);
        ctx.waitTicks(3);
    }

    static float[] center(float[] b) {
        return new float[] {b[0] + b[2] / 2, b[1] + b[3] / 2};
    }

    static void resetAll(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            EditMode.leave();
            Screens.store().clear();
        });
    }

    static String fmt(float[] b) {
        return b == null ? "null" : String.format(Locale.ROOT, "[%.0f,%.0f %.0fx%.0f]", b[0], b[1], b[2], b[3]);
    }
}
