package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.screens.edit.EditMode;
import dev.henny.vibeyclient.module.screens.edit.Pencil;
import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.Placement;
import dev.henny.vibeyclient.module.screens.core.ScreenLayout;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;

import static dev.henny.vibeyclient.gametest.ScreensTestKit.*;

final class ScreensOptionsChecks {
    private ScreensOptionsChecks() {}

    static void open(ClientGameTestContext ctx) {
        ctx.setScreen(() -> ClientCompat.optionsScreen(null));
        ctx.waitTicks(5);
    }

    static void run(ClientGameTestContext ctx) {
        open(ctx);
        check(ctx.computeOnClient(mc -> mc.screen instanceof OptionsScreen), "options did not open");
        log("options widgets: %s", ids(ctx));
        Box pencil = ctx.computeOnClient(mc -> Pencil.box(session(mc)));
        hover(ctx, pencil.cx(), pencil.cy());
        shot(ctx, "options_pencil");
        click(ctx, pencil.cx(), pencil.cy(), 0);
        check(editing(ctx), "pencil click did not start edit mode");
        hover(ctx, 20, 200);
        shot(ctx, "options_edit");
        moveAndScale(ctx);
        clickScaled(ctx);
        screenScale(ctx);
        style(ctx);
        resetScreen(ctx);
    }

    private static void moveAndScale(ClientGameTestContext ctx) {
        String video = find(ctx, "options.video");
        float[] b = box(ctx, video);
        float[] c = center(b);
        drag(ctx, c[0], c[1], c[0] - 60, c[1] - 55);
        float[] moved = box(ctx, video);
        log("video %s -> %s", fmt(b), fmt(moved));
        check(Math.abs(moved[1] - b[1] + 55) <= 5, "video button did not move up: " + fmt(moved));
        float[] mid = center(moved);
        hover(ctx, mid[0], mid[1]);
        ctx.getInput().scroll(1);
        ctx.waitTicks(2);
        ctx.getInput().scroll(1);
        ctx.waitTicks(3);
        check(placement(ctx, video).scale() == 1.25f, "wheel did not scale to 125%: " + placement(ctx, video).scale());
        shot(ctx, "options_moved_scaled");
        int z = ctx.computeOnClient(mc -> ClientCompat.layoutLetter(Keys.Z) == Keys.Z ? Keys.Z : Keys.Y);
        ctx.getInput().holdControl();
        ctx.getInput().pressKey(z);
        ctx.getInput().releaseControl();
        ctx.waitTicks(3);
        float undone = placement(ctx, video).scale();
        log("after undo: %.2f", undone);
        check(undone == 1f, "Ctrl+Z did not undo the merged wheel steps: " + undone);
        check(placement(ctx, video).pos() != null, "Ctrl+Z also undid the move");
        ctx.getInput().scroll(1);
        ctx.waitTicks(2);
        ctx.getInput().scroll(1);
        ctx.waitTicks(3);
        check(placement(ctx, video).scale() == 1.25f, "wheel after undo");
    }

    private static void clickScaled(ClientGameTestContext ctx) {
        String video = find(ctx, "options.video");
        ctx.getInput().pressKey(Keys.ESCAPE);
        ctx.waitTicks(3);
        check(!editing(ctx), "Esc did not leave edit mode");
        check(ctx.computeOnClient(mc -> mc.screen instanceof OptionsScreen), "Esc in edit mode closed the screen");
        float[] v = box(ctx, video);
        check(Math.round(v[2]) == 188 && Math.round(v[3]) == 25, "scaled size is not 188x25: " + fmt(v));
        hover(ctx, v[0] + v[2] - 6, v[1] + v[3] - 3);
        shot(ctx, "options_scaled_hover");
        ctx.getInput().pressMouse(0);
        ctx.waitTicks(5);
        check(ctx.computeOnClient(mc -> mc.screen instanceof VideoSettingsScreen),
                "click on the scaled corner did not open video settings: " + ctx.computeOnClient(mc -> String.valueOf(mc.screen)));
        open(ctx);
        float[] again = box(ctx, video);
        check(again != null && Math.abs(again[0] - v[0]) < 1 && Math.abs(again[1] - v[1]) < 1, "layout lost after reopen: " + fmt(again));
    }

    private static void screenScale(ClientGameTestContext ctx) {
        ctx.getInput().pressKey(Keys.F12);
        ctx.waitTicks(3);
        check(editing(ctx), "F12 did not start edit mode");
        Box chip = ctx.computeOnClient(mc -> EditMode.current().toolbar().box(dev.henny.vibeyclient.module.screens.edit.Toolbar.Kind.SCALE));
        for (int i = 0; i < 2; i++) {
            hover(ctx, chip.x() + 3, chip.cy());
            ctx.getInput().pressMouse(0);
            ctx.waitTicks(4);
            chip = ctx.computeOnClient(mc -> EditMode.current().toolbar().box(dev.henny.vibeyclient.module.screens.edit.Toolbar.Kind.SCALE));
        }
        float k = layout(ctx).scale();
        check(k == 0.75f, "two clicks on Size - did not give 75%: " + k);
        int[] size = ctx.computeOnClient(mc -> new int[] {mc.screen.width, mc.screen.height});
        check(size[0] == 853, "screen not laid out at 75% (width " + size[0] + ")");
        shot(ctx, "options_screen_75");
        ctx.getInput().pressKey(Keys.ESCAPE);
        ctx.waitTicks(3);
        String sounds = find(ctx, "options.sounds");
        float[] s = box(ctx, sounds);
        click(ctx, s[0] + s[2] - 4, s[1] + s[3] / 2, 0);
        check(ctx.computeOnClient(mc -> mc.screen instanceof SoundOptionsScreen), "click at 75% did not open sounds");
        open(ctx);
    }

    private static void style(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            var s = session(mc);
            ScreenLayout l = s.layout().withDim(25).withBlur(0).withHideTitle(true);
            dev.henny.vibeyclient.module.screens.edit.EditActions.store(s, l);
        });
        ctx.waitTicks(3);
        shot(ctx, "options_style_title_hidden");
    }

    private static void resetScreen(ClientGameTestContext ctx) {
        ctx.getInput().pressKey(Keys.F12);
        ctx.waitTicks(3);
        hover(ctx, 6, 180);
        ctx.getInput().pressMouse(1);
        ctx.waitTicks(3);
        java.util.List<String> labels = ctx.computeOnClient(mc -> EditMode.current().menuOpen()
                ? java.util.List.of("open") : java.util.List.of());
        check(!labels.isEmpty(), "right click did not open the menu");
        shot(ctx, "options_context_menu");
        ctx.getInput().pressKey(Keys.ESCAPE);
        ctx.waitTicks(2);
        float[] tb = ctx.computeOnClient(mc -> {
            Box b = EditMode.current().toolbar().box(dev.henny.vibeyclient.module.screens.edit.Toolbar.Kind.RESET);
            return new float[] {b.x(), b.y(), b.w(), b.h()};
        });
        float[] c = center(tb);
        click(ctx, c[0], c[1], 0);
        ctx.waitTicks(3);
        check(layout(ctx).isEmpty(), "Reset did not clear the screen: " + layout(ctx));
        check(placement(ctx, find(ctx, "options.video")).equals(Placement.DEFAULT), "video still placed");
        ctx.getInput().pressKey(Keys.ESCAPE);
        ctx.waitTicks(2);
        ctx.setScreen(() -> null);
    }
}
