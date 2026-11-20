package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.chat.core.ChatEntry;
import dev.henny.vibeyclient.module.chat.filters.FilterScreens;
import dev.henny.vibeyclient.module.chat.hook.ChatHooks;
import dev.henny.vibeyclient.module.chat.line.MentionTag;
import dev.henny.vibeyclient.module.chat.line.Mentions;
import dev.henny.vibeyclient.module.chat.notify.ChatToasts;
import dev.henny.vui.host.Toast;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.host.VuiScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

import static dev.henny.vibeyclient.gametest.ChatTestKit.*;

final class ChatLineChecks {
    private ChatLineChecks() {}

    static void timestamps(ClientGameTestContext ctx) {
        say(ctx, "<Alex> hi everyone");
        say(ctx, "<Sam> found diamond ore at 120 12 -40");
        say(ctx, "<Alex> nice, more diamond please");
        say(ctx, "Server restarts in 10 minutes");
        String first = lines(ctx).get(0);
        log("newest line: %s", first);
        check(first.matches("^\\[\\d{2}:\\d{2}] Server restarts in 10 minutes$"), "newest line has no timestamp: " + first);
        ctx.runOnClient(mc -> module().timestamps.set(false));
        ctx.waitTicks(3);
        check(lines(ctx).get(0).equals("Server restarts in 10 minutes"), "timestamps off should drop the prefix: " + lines(ctx).get(0));
        ctx.runOnClient(mc -> module().timestamps.set(true));
        ctx.waitTicks(3);
        check(lines(ctx).get(0).startsWith("["), "timestamps back on");
    }

    static void mention(ClientGameTestContext ctx) {
        String own = ctx.computeOnClient(mc -> Mentions.ownName());
        say(ctx, "<Alex> hey " + own + ", come to spawn");
        ctx.waitTicks(4);
        ChatEntry e = ctx.computeOnClient(mc -> ChatHooks.INDEX.entry(chat(mc).vibey$allMessages().get(0)));
        check(e != null && e.mention(), "own name should count as a mention");
        GuiMessage.Line line = ctx.computeOnClient(mc -> chat(mc).vibey$trimmedMessages().get(0));
        check(line.tag() == MentionTag.TAG, "mention line should carry the mention tag");
        Toast t = ctx.computeOnClient(mc -> ChatToasts.last());
        check(t != null && t.getTitle().equals("Mentioned"), "mention toast expected, got " + (t == null ? null : t.getTitle()));
        shot(ctx, "02_mention_toast");
        say(ctx, "<" + own + "> I said my own name " + own);
        ChatEntry self = ctx.computeOnClient(mc -> ChatHooks.INDEX.entry(chat(mc).vibey$allMessages().get(0)));
        check(!self.mention(), "own messages are no mention");
    }

    static void directMessage(ClientGameTestContext ctx) {
        say(ctx, Component.translatable("commands.message.display.incoming", "Alex", "secret base at 10 64 10"));
        ctx.waitTicks(3);
        Toast t = ctx.computeOnClient(mc -> ChatToasts.last());
        check(t != null && t.getTitle().equals("Alex") && t.getAction().equals("Reply"), "DM toast with reply expected");
        shot(ctx, "03_dm_toast");
        ctx.runOnClient(mc -> VuiMc.toasts().stack().act(t));
        ctx.waitTicks(4);
        String input = ctx.computeOnClient(mc -> mc.screen instanceof ChatScreen s ? ChatTestKit.input(s).getValue() : null);
        check("/msg Alex ".equals(input), "reply should open the chat with /msg Alex, got " + input);
        ctx.runOnClient(mc -> mc.setScreen(null));
        ctx.waitTicks(2);
    }

    static void collapse(ClientGameTestContext ctx) {
        int before = messageCount(ctx);
        for (int i = 0; i < 4; i++) say(ctx, "Buy cheap ranks at shop.example");
        int after = messageCount(ctx);
        check(after == before + 1, "4 repeats should be one message, " + before + " -> " + after);
        String line = lines(ctx).get(0);
        check(line.endsWith("×4"), "collapsed line should end with x4: " + line);
    }

    static void filter(ClientGameTestContext ctx) {
        check(ctx.computeOnClient(mc -> module().filterStore().add("discord.gg")), "add filter");
        int before = messageCount(ctx);
        say(ctx, "<Spammer> join discord.gg/free-stuff now");
        check(messageCount(ctx) == before, "filtered line must not be added");
        check(!String.join("\n", lines(ctx)).contains("discord.gg"), "filtered text visible");
        VuiScreen s = ctx.computeOnClient(mc -> FilterScreens.open());
        ctx.waitTicks(6);
        String errors = ctx.computeOnClient(mc -> s.document().diagnostics().format());
        check(errors.isBlank(), "filter editor has errors: " + errors);
        shot(ctx, "05_filter_editor");
        ctx.runOnClient(mc -> mc.setScreen(null));
        ctx.waitTicks(2);
    }
}
