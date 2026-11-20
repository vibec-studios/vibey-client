package dev.henny.vibeyclient.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.client.VibeyPlayers;
import dev.henny.vibeyclient.client.ranks.Ranks;
import dev.henny.vibeyclient.module.nametags.NameIcons;
import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.module.social.ui.SocialScreen;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class RanksGameTest implements FabricClientGameTest {
    private static final String P = "[vibeyclient-ranks-test] ";
    private static final int SEC = 20;

    private record Bot(UUID id, String name, String rank, String style) {}

    @Override
    public void runTest(ClientGameTestContext ctx) {
        String seed = System.getProperty("vibeyclient.ranksSeed");
        if (seed == null) return;
        List<Bot> bots = bots(seed);
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
        });
        ctx.waitFor(mc -> Services.get(VibeBackend.class).map(VibeBackend::isReady).orElse(false), 90 * SEC);
        ctx.waitFor(mc -> Ranks.doc().ranks().size() >= 8 && Ranks.own().known(), 30 * SEC);
        ctx.runOnClient(mc -> System.out.println(P + "ranks " + Ranks.doc().version() + ", own " + Ranks.own()));
        page(ctx, "ranks_01_page_en");
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
        page(ctx, "ranks_02_page_de");
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        social(ctx, bots);
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeyranks")).create()) {
            world.getClientWorld().waitForChunksRender();
            inWorld(ctx, world, bots);
        }
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
    }

    private static void inWorld(ClientGameTestContext ctx, TestSingleplayerContext world, List<Bot> bots) {
        int[] p = ctx.computeOnClient(mc -> new int[] {(int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getZ())});
        int x = p[0], z = p[1], y = 150;
        for (String c : new String[] {"gamemode creative @a", "time set 6000", "weather clear",
                String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air", x - 10, y + 1, z - 2, x + 10, y + 8, z + 10),
                String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:grass_block", x - 10, y, z - 2, x + 10, y, z + 10),
                String.format(Locale.ROOT, "tp @a %.1f %d %.1f 0 8", x + 0.5, y + 1, z + 0.5)}) {
            world.getServer().runCommand(c);
        }
        ctx.waitTicks(30);
        world.getClientWorld().waitForChunksRender();
        ctx.runOnClient(mc -> {
            mc.player.setYRot(0);
            mc.player.setXRot(8);
            for (int i = 0; i < bots.size(); i++) {
                Bot b = bots.get(i);
                try {
                    FakePlayers.spawn(mc, b.id(), b.name(), x + 0.5 + (i - (bots.size() - 1) / 2.0) * 2.8, y + 1, z + 7.5, 180f);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        ctx.waitFor(mc -> bots.stream().allMatch(b -> b.rank().equals(VibeyPlayers.identity(b.id()).rank())), 30 * SEC);
        ctx.runOnClient(mc -> bots.forEach(b -> System.out.println(P + b.name() + " -> " + VibeyPlayers.identity(b.id()))));
        ctx.waitTicks(10);
        ctx.takeScreenshot("ranks_10_nametags_a");
        ctx.waitTicks(7);
        ctx.takeScreenshot("ranks_11_nametags_b");
        ctx.getInput().holdKey(o -> o.keyPlayerList);
        ctx.waitTicks(4);
        ctx.takeScreenshot("ranks_12_tablist");
        ctx.getInput().releaseKey(o -> o.keyPlayerList);
        chat(ctx, bots);
        ctx.takeScreenshot("ranks_13_chat");
        int animated = ctx.computeOnClient(mc -> NameIcons.animatedCount());
        System.out.println(P + "animated nametags: " + animated);
        if (animated != bots.size()) throw new AssertionError("animated nametags " + animated + " != " + bots.size());
        ctx.runOnClient(mc -> ServerRules.apply(List.of("nametags.ranks", "nametags.styles"), "test"));
        ctx.waitTicks(4);
        ctx.takeScreenshot("ranks_14_rules_seal_only");
        ctx.runOnClient(mc -> ServerRules.clear());
    }

    private static void chat(ClientGameTestContext ctx, List<Bot> bots) {
        ctx.runOnClient(mc -> {
            mc.gui.getChat().clearMessages(false);
            String[] lines = {"hi everyone!", "welcome to the server", "nice build", "gg"};
            for (int i = 0; i < bots.size(); i++) {
                Bot b = bots.get(i);
                ChatType.Bound bound = ChatType.bind(ChatType.CHAT, mc.level.registryAccess(), Component.literal(b.name()));
                mc.getChatListener().handlePlayerChatMessage(PlayerChatMessage.system(lines[i % lines.length]), FakePlayers.profile(b.id(), b.name()), bound);
            }
        });
        ctx.waitTicks(6);
    }

    private static void page(ClientGameTestContext ctx, String name) {
        ModulesPanel.preselect("nametags");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        ctx.takeScreenshot(name);
        ctx.setScreen(() -> null);
    }

    private static void social(ClientGameTestContext ctx, List<Bot> bots) {
        SocialModule m = ctx.computeOnClient(mc -> ModuleManager.get().get(SocialModule.class));
        if (m == null) return;
        ctx.setScreen(() -> new SocialScreen(null, m, SidebarRoute.tab(SidebarRoute.Tab.FRIENDS)));
        ctx.waitTicks(30);
        ctx.takeScreenshot("ranks_03_friends");
        ctx.setScreen(() -> new SocialScreen(null, m, SidebarRoute.profile(bots.get(0).id().toString())));
        ctx.waitTicks(20);
        ctx.takeScreenshot("ranks_04_profile");
        ctx.setScreen(() -> null);
    }

    private static List<Bot> bots(String seed) {
        try {
            JsonObject o = JsonParser.parseString(Files.readString(Path.of(seed))).getAsJsonObject();
            List<Bot> out = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("bots")) {
                JsonObject b = e.getAsJsonObject();
                out.add(new Bot(UUID.fromString(b.get("id").getAsString()), b.get("name").getAsString(),
                        b.getAsJsonArray("ranks").get(0).getAsString(), b.get("style").getAsString()));
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("ranks seed unreadable: " + seed, e);
        }
    }
}
