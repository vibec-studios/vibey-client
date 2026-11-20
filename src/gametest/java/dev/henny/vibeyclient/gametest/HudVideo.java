package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudTemplates;
import dev.henny.vibeyclient.ui.hud.editor.HudEditorScreen;

import java.nio.file.Path;
import java.util.Locale;

final class HudVideo {
    private final HudTestKit t;
    private final Path frames;
    private final double[] cur = {320, 120};
    private int frame;

    private HudVideo(HudTestKit t) {
        this.t = t;
        this.frames = t.review.resolve("frames");
    }

    static void run(HudTestKit t) {
        new HudVideo(t).record();
    }

    private void shot() {
        Path f = t.context.takeScreenshot(String.format(Locale.ROOT, "hudvid_%04d", frame));
        HudTestKit.copy(f, frames, String.format(Locale.ROOT, "f_%04d.png", frame++));
    }

    private void record() {
        t.context.runOnClient(mc -> {
            HudTemplates.apply("pvp");
            HudEditorScreen.drawPointer = true;
        });
        t.openEditor();
        hold(6);
        HudLayout.Box f = t.box("hud.fps");
        glide(f.x + f.w / 2, f.y + f.h / 2, 10);
        press();
        glide(t.sw() / 2.0 + 2, f.y + f.h / 2 + 2, 22);
        hold(8);
        glide(t.sw() / 2.0 + 40, f.y + 22, 14);
        release();
        HudLayout.Box a = t.box("hud.armor");
        glide(a.x + a.w / 2, a.y + a.h / 2, 12);
        for (int i = 0; i < 3; i++) {
            t.context.getInput().scroll(1);
            hold(6);
        }
        t.context.getInput().scroll(-1);
        hold(8);
        HudLayout.Box c = t.box("hud.cps");
        HudLayout.Box k = t.box("hud.keystrokes");
        glide(k.x + k.w + 50, k.y + k.h + 6, 10);
        press();
        glide(1, c.y - 2, 14);
        release();
        glide(k.x + k.w / 2, k.y + k.h / 2, 8);
        press();
        glide(k.x + k.w / 2 + 40, k.y + k.h / 2 + 10, 14);
        release();
        float[] presets = t.center("dock-presets");
        glide(presets[0], presets[1], 12);
        t.context.getInput().pressMouse(0);
        hold(10);
        float[] building = t.center("preset-building");
        glide(building[0], building[1], 10);
        t.context.getInput().pressMouse(0);
        hold(24);
        System.out.println(HudTestKit.LOG + "video frames: " + frame);
        t.context.setScreen(() -> null);
        t.context.runOnClient(mc -> HudEditorScreen.drawPointer = false);
    }

    private void press() {
        t.context.getInput().holdMouse(0);
        hold(2);
    }

    private void release() {
        hold(4);
        t.context.getInput().releaseMouse(0);
        hold(4);
    }

    private void glide(double x, double y, int steps) {
        double gs = t.gs();
        double x0 = cur[0], y0 = cur[1];
        for (int i = 1; i <= steps; i++) {
            double p = i / (double) steps;
            double e = p < 0.5 ? 4 * p * p * p : 1 - Math.pow(-2 * p + 2, 3) / 2;
            cur[0] = x0 + (x - x0) * e;
            cur[1] = y0 + (y - y0) * e;
            t.context.getInput().setCursorPos(cur[0] * gs, cur[1] * gs);
            t.context.waitTick();
            shot();
        }
    }

    private void hold(int ticks) {
        for (int i = 0; i < ticks; i++) {
            t.context.waitTick();
            shot();
        }
    }
}
