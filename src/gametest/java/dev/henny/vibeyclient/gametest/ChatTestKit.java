package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.chat.ChatModule;
import dev.henny.vibeyclient.module.chat.hook.ChatHooks;
import dev.henny.vibeyclient.module.chat.mixin.ChatComponentAccessor;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ChatTestKit {
    static final String P = "[chat-gametest] ";
    static final int LEFT = 0, ESCAPE = 256, KEY_F = 70, KEY_F7 = 296, TAB = 258;

    private ChatTestKit() {}

    static ChatModule module() {
        return ChatModule.get();
    }

    static ChatComponentAccessor chat(Minecraft mc) {
        return (ChatComponentAccessor) mc.gui.getChat();
    }

    static void say(ClientGameTestContext ctx, Component c) {
        ctx.runOnClient(mc -> mc.gui.getChat().addMessage(c));
        ctx.waitTicks(1);
    }

    static void say(ClientGameTestContext ctx, String text) {
        say(ctx, Component.literal(text));
    }

    static List<String> lines(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> {
            List<String> out = new ArrayList<>();
            for (GuiMessage.Line l : chat(mc).vibey$trimmedMessages()) {
                StringBuilder sb = new StringBuilder();
                l.content().accept((i, s, cp) -> {
                    sb.appendCodePoint(cp);
                    return true;
                });
                out.add(sb.toString());
            }
            return out;
        });
    }

    static int messageCount(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> chat(mc).vibey$allMessages().size());
    }

    static String newestPlain(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> ChatHooks.plain(chat(mc).vibey$allMessages().get(0)));
    }

    static void shot(ClientGameTestContext ctx, String name) {
        ctx.waitTicks(2);
        Path file = ctx.takeScreenshot("chat_" + name);
        Path review = WaypointsTestKit.root().resolve("build/review/chat");
        try {
            Files.createDirectories(review);
            Files.copy(file, review.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AssertionError("copy screenshot " + name, e);
        }
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(P + message);
    }

    static void log(String format, Object... args) {
        System.out.println(P + String.format(Locale.ROOT, format, args));
    }

    static EditBox input(Screen s) {
        for (var c : s.children()) if (c instanceof EditBox b) return b;
        throw new AssertionError(P + "no input box on " + s);
    }

    static void cursor(ClientGameTestContext ctx, double guiX, double guiY) {
        double gs = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
        ctx.getInput().setCursorPos(guiX * gs, guiY * gs);
        ctx.waitTicks(2);
    }
}
