package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.privacy.Privacy;
import dev.henny.vibeyclient.module.chat.line.Mentions;
import dev.henny.vibeyclient.module.social.SocialModule;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;

import java.util.UUID;

import static dev.henny.vibeyclient.gametest.ChatTestKit.*;

final class ChatStreamerChecks {
    static final UUID ALEX = UUID.fromString("6c0d1f4e-2b3a-4c5d-8e9f-0a1b2c3d4e5f");
    static final String NAME = "Alexander";

    private ChatStreamerChecks() {}

    static void spawnFriend(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            try {
                var p = mc.player;
                double yaw = Math.toRadians(p.getYRot());
                FakePlayers.spawn(mc, ALEX, NAME, p.getX() - Math.sin(yaw) * 3, p.getY(), p.getZ() + Math.cos(yaw) * 3, p.getYRot() + 180);
            } catch (Exception e) {
                throw new AssertionError(P + "spawn fake player", e);
            }
        });
        ctx.waitTicks(4);
    }

    static void streamer(ClientGameTestContext ctx) {
        String own = ctx.computeOnClient(mc -> Mentions.ownName());
        say(ctx, "<" + NAME + "> hi " + own + ", nice base");
        check(lines(ctx).get(0).contains(NAME), "name visible before streamer mode");
        ctx.runOnClient(mc -> {
            SocialModule s = (SocialModule) ModuleManager.get().get("social");
            if (!s.isEnabled()) throw new AssertionError(P + "social module off: " + s.status());
        });
        ctx.getInput().pressKey(KEY_F7);
        ctx.waitTicks(30);
        check(ctx.computeOnClient(mc -> Privacy.hideNames() && Privacy.hideSkins() && Privacy.hideServer()), "F7 should turn streamer mode on");
        String all = String.join("\n", lines(ctx));
        log("streamer chat: %s", lines(ctx).get(0));
        check(!all.contains(NAME) && !all.contains(own), "names must be hidden in chat: " + lines(ctx).get(0));
        check(lines(ctx).get(0).contains("Player 1") && lines(ctx).get(0).contains("Me"), "aliases expected: " + lines(ctx).get(0));
        String tab = ctx.computeOnClient(mc -> mc.gui.getTabList().getNameForDisplay(info(mc)).getString());
        check(!tab.contains(NAME) && tab.startsWith("Player"), "tab list shows " + tab);
        boolean skin = ctx.computeOnClient(mc -> info(mc).getSkin().equals(DefaultPlayerSkin.getDefaultSkin()));
        check(skin, "skins should fall back to the default");
        boolean toastsGone = ctx.computeOnClient(mc -> dev.henny.vui.mc.VuiMc.toasts().stack().toasts().size() == 0);
        check(toastsGone, "chat toasts with names should close when names get hidden");
        ctx.getInput().holdKey(TAB);
        shot(ctx, "12_streamer_mode");
        ctx.getInput().releaseKey(TAB);
        ctx.getInput().pressKey(KEY_F7);
        ctx.waitTicks(30);
        check(ctx.computeOnClient(mc -> !Privacy.hideNames()), "F7 again turns it off");
        check(lines(ctx).get(0).contains(NAME), "names back after streamer mode");
    }

    private static PlayerInfo info(net.minecraft.client.Minecraft mc) {
        PlayerInfo i = mc.getConnection().getPlayerInfo(ALEX);
        if (i == null) throw new AssertionError(P + "fake player info missing");
        return i;
    }
}
