package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.screens.Screens;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;

import static dev.henny.vibeyclient.gametest.ScreensTestKit.*;

final class ScreensTitleChecks {
    private ScreensTitleChecks() {}

    static void run(ClientGameTestContext ctx) {
        ctx.setScreen(TitleScreen::new);
        ctx.waitTicks(10);
        log("title widgets: %s", ids(ctx));
        ctx.getInput().pressKey(Keys.F12);
        ctx.waitTicks(3);
        check(editing(ctx), "F12 did not start edit mode on the title screen");
        String single = find(ctx, "menu.singleplayer");
        float[] b = box(ctx, single);
        float[] c = center(b);
        drag(ctx, c[0], c[1], c[0] - 150, c[1] - 30);
        hover(ctx, c[0] - 150, c[1] - 30);
        ctx.getInput().scroll(1);
        ctx.waitTicks(3);
        String online = find(ctx, "menu.online");
        float[] o = box(ctx, online);
        click(ctx, o[0] + o[2] / 2, o[1] + o[3] / 2, 1);
        float[] first = ctx.computeOnClient(mc -> {
            var r = dev.henny.vibeyclient.module.screens.edit.EditMode.current();
            return r.menuOpen() ? new float[] {1} : null;
        });
        check(first != null, "right click on Realms did not open the menu");
        shot(ctx, "title_menu");
        click(ctx, o[0] + o[2] / 2 + 6, o[1] + o[3] / 2 + 5, 0);
        check(placement(ctx, online).hidden(), "Hide from the menu did not hide Realms");
        hover(ctx, 3, 3);
        shot(ctx, "title_edit");
        ctx.getInput().pressKey(Keys.ESCAPE);
        ctx.waitTicks(3);
        float[] moved = box(ctx, single);
        log("singleplayer %s -> %s", fmt(b), fmt(moved));
        check(moved[0] < b[0] - 100, "singleplayer did not move left");
        boolean realmsVisible = ctx.computeOnClient(mc -> session(mc).item(online).widget.visible);
        check(!realmsVisible, "hidden Realms button is still visible");
        hover(ctx, 3, 3);
        shot(ctx, "title_layout");
        float[] mc = center(moved);
        click(ctx, mc[0], mc[1], 0);
        check(ctx.computeOnClient(m -> !(m.screen instanceof TitleScreen)), "click on the moved singleplayer button failed");
        ctx.setScreen(TitleScreen::new);
        ctx.waitTicks(5);
        ctx.runOnClient(m -> Screens.store().remove(session(m).name));
        ctx.setScreen(TitleScreen::new);
        ctx.waitTicks(5);
        float[] back = box(ctx, single);
        check(Math.abs(back[0] - b[0]) < 1, "title layout not reset: " + fmt(back));
    }
}
