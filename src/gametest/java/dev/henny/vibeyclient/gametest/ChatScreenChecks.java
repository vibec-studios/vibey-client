package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.chat.screen.CopyButton;
import dev.henny.vibeyclient.module.chat.screen.LineHit;
import dev.henny.vibeyclient.module.chat.screen.SearchMode;
import dev.henny.vibeyclient.module.chat.window.ChatSize;
import dev.henny.vibeyclient.module.chat.window.ChatTransform;
import dev.henny.vibeyclient.module.chat.window.ChatWindow;
import dev.henny.vibeyclient.ui.hud.HudEditorScreen;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;

import java.util.List;

import static dev.henny.vibeyclient.gametest.ChatTestKit.*;

final class ChatScreenChecks {
    private ChatScreenChecks() {}

    static void openChat(ClientGameTestContext ctx) {
        ctx.setScreen(() -> new ChatScreen("", false));
        ctx.waitTicks(4);
    }

    static void search(ClientGameTestContext ctx) {
        openChat(ctx);
        ctx.getInput().holdControl();
        ctx.getInput().pressKey(KEY_F);
        ctx.getInput().releaseControl();
        ctx.waitTicks(3);
        check(ctx.computeOnClient(mc -> SearchMode.active()), "Ctrl+F should start the search");
        ctx.getInput().typeChars("diamond");
        ctx.waitTicks(4);
        List<String> shown = lines(ctx);
        log("search shows %d lines", shown.size());
        check(shown.size() == 2 && shown.stream().allMatch(l -> l.toLowerCase().contains("diamond")), "search should show 2 diamond lines: " + shown);
        int hits = ctx.computeOnClient(mc -> SearchMode.count());
        check(hits == 2, "hit count " + hits);
        String bar = ctx.computeOnClient(mc -> SearchMode.controller().hits.peek());
        check(bar.equals("2 found"), "search bar text: " + bar);
        String errors = ctx.computeOnClient(mc -> dev.henny.vui.mc.VuiMc.overlays().top().diagnostics().format());
        check(errors.isBlank(), "search bar document has errors: " + errors);
        shot(ctx, "06_search");
        ctx.getInput().pressKey(ESCAPE);
        ctx.waitTicks(3);
        check(ctx.computeOnClient(mc -> !SearchMode.active() && mc.screen instanceof ChatScreen), "Esc should end the search, chat stays");
        check(lines(ctx).size() > 4, "all lines back after the search");
    }

    static double[] copyPoint(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> {
            float f = ChatSize.scale();
            int m = Mth.floor((ChatSize.guiHeight() - ChatSize.BOTTOM) / f);
            int p = ChatSize.lineHeight();
            double x = ChatSize.localWidth() + 8 - 5, y = m - p / 2.0;
            ChatTransform t = ChatWindow.transform();
            double sx = (x + 4) * f, sy = y * f;
            if (t.identity()) return new double[] {sx, sy};
            return new double[] {t.x + t.scale * sx, t.y + t.scale * (sy - t.anchorY)};
        });
    }

    static void copy(ClientGameTestContext ctx, String shotName) {
        openChat(ctx);
        double[] at = copyPoint(ctx);
        cursor(ctx, at[0], at[1]);
        LineHit hit = ctx.computeOnClient(mc -> dev.henny.vibeyclient.module.chat.screen.ChatGeometry.hit(at[0], at[1]));
        check(hit != null && hit.onButton(), "cursor should be on the copy button of the newest line");
        shot(ctx, shotName);
        String newest = newestPlain(ctx);
        ctx.getInput().pressMouse(LEFT);
        ctx.waitTicks(3);
        String copied = ctx.computeOnClient(mc -> CopyButton.lastCopied());
        String clip = ctx.computeOnClient(mc -> mc.keyboardHandler.getClipboard());
        check(newest.equals(copied) && newest.equals(clip), "copied '" + copied + "' / clipboard '" + clip + "', expected '" + newest + "'");
        ctx.runOnClient(mc -> mc.setScreen(null));
        ctx.waitTicks(2);
    }

    static void moveInEditor(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> HudLayout.setFlag("intro", true));
        ctx.setScreen(() -> new HudEditorScreen(null));
        ctx.waitTicks(8);
        HudLayout.Box b = ctx.computeOnClient(mc -> HudLayout.box(ChatWindow.element(), ChatSize.guiWidth(), ChatSize.guiHeight(), new HudLayout.Box()));
        log("chat box before %.0f %.0f %.0fx%.0f", b.x, b.y, b.w, b.h);
        double sx = b.x + b.w / 2, sy = b.y + b.h / 2, tx = sx + 260, ty = sy - 130;
        cursor(ctx, sx, sy);
        ctx.getInput().holdMouse(LEFT);
        ctx.waitTicks(1);
        for (int i = 1; i <= 10; i++) cursor(ctx, sx + (tx - sx) * i / 10, sy + (ty - sy) * i / 10);
        shot(ctx, "08_editor_drag");
        ctx.getInput().releaseMouse(LEFT);
        ctx.waitTicks(2);
        ctx.getInput().scroll(-1);
        ctx.waitTicks(3);
        HudLayout.Box a = ctx.computeOnClient(mc -> HudLayout.box(ChatWindow.element(), ChatSize.guiWidth(), ChatSize.guiHeight(), new HudLayout.Box()));
        log("chat box after %.0f %.0f %.0fx%.0f scale %.2f", a.x, a.y, a.w, a.h, a.scale);
        check(a.x > b.x + 150 && a.y < b.y - 60, "chat should have moved right and up");
        check(a.scale < 1f, "wheel down should shrink the chat");
        shot(ctx, "09_editor_moved");
        ctx.getInput().pressKey(ESCAPE);
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> mc.screen == null), "Esc saves and leaves the editor");
    }

    static void movedChatWorks(ClientGameTestContext ctx) {
        say(ctx, "<Sam> the chat moved up here");
        shot(ctx, "10_chat_moved_hud");
        copy(ctx, "11_chat_moved_copy");
    }
}
