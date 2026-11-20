package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.privacy.Privacy;
import dev.henny.vibeyclient.module.social.SocialModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;

import java.lang.reflect.Field;
import java.util.List;

import static dev.henny.vibeyclient.gametest.ChatTestKit.*;

public final class ChatGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            if (!ModuleManager.get().isEnabled("chat")) throw new AssertionError(P + "chat module off");
        });
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibey-chat")).create()) {
            world.getClientWorld().waitForChunksRender();
            for (String c : new String[] {"time set 6000", "weather clear", "gamerule doDaylightCycle false", "difficulty peaceful"}) {
                world.getServer().runCommand(c);
            }
            ctx.runOnClient(mc -> mc.gui.getChat().clearMessages(false));
            ctx.waitTicks(10);
            ChatLineChecks.timestamps(ctx);
            ChatScreenChecks.openChat(ctx);
            shot(ctx, "01_timestamps");
            ctx.runOnClient(mc -> mc.setScreen(null));
            ChatLineChecks.mention(ctx);
            ChatLineChecks.directMessage(ctx);
            ChatLineChecks.collapse(ctx);
            ChatScreenChecks.openChat(ctx);
            shot(ctx, "04_collapsed");
            ctx.runOnClient(mc -> mc.setScreen(null));
            ChatLineChecks.filter(ctx);
            ChatScreenChecks.search(ctx);
            ChatScreenChecks.copy(ctx, "07_copy_hover");
            ChatStreamerChecks.spawnFriend(ctx);
            ChatStreamerChecks.streamer(ctx);
            ChatScreenChecks.moveInEditor(ctx);
            ChatScreenChecks.movedChatWorks(ctx);
        }
        addressHidden(ctx);
        log("all chat checks passed");
    }

    private static void addressHidden(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            ((SocialModule) ModuleManager.get().get("social")).streamer.set(true);
            mc.options.lastMpIp = "secret.example.net";
        });
        ctx.waitTicks(2);
        check(ctx.computeOnClient(mc -> Privacy.hideServer()), "streamer hides the server address");
        ctx.setScreen(() -> new DirectJoinServerScreen(new TitleScreen(), ok -> {}, new ServerData("x", "", ServerData.Type.OTHER)));
        ctx.waitTicks(4);
        int formatters = ctx.computeOnClient(mc -> formatterCount(input(mc.screen)));
        check(formatters == 1, "address box should get the mask formatter, has " + formatters);
        shot(ctx, "13_address_hidden");
        ctx.runOnClient(mc -> ((SocialModule) ModuleManager.get().get("social")).streamer.set(false));
        ctx.setScreen(() -> new TitleScreen());
    }

    private static int formatterCount(EditBox box) {
        try {
            for (Field f : EditBox.class.getDeclaredFields()) {
                if (f.getType() == List.class) {
                    f.setAccessible(true);
                    return ((List<?>) f.get(box)).size();
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(P + "formatters", e);
        }
        return -1;
    }
}
