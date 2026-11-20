package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.servers.ServersModule;
import dev.henny.vibeyclient.module.servers.core.WorldSort;
import dev.henny.vibeyclient.module.servers.mixin.SelectWorldAccess;
import dev.henny.vibeyclient.module.servers.worlds.WorldOrderStore;
import dev.henny.vibeyclient.module.servers.worlds.WorldScreenHooks;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;

import java.util.ArrayList;
import java.util.List;

import static dev.henny.vibeyclient.gametest.ServerListsKit.SHOTS;
import static dev.henny.vibeyclient.gametest.ServerListsKit.check;
import static dev.henny.vibeyclient.gametest.ServerListsKit.log;

final class WorldOrderChecks {
    static final String A = "vibey_dnd_a", B = "vibey_dnd_b", C = "vibey_dnd_c";

    private final ClientGameTestContext ctx;

    WorldOrderChecks(ClientGameTestContext ctx) {
        this.ctx = ctx;
    }

    void run() {
        makeWorlds();
        ctx.runOnClient(mc -> {
            WorldOrderStore.set(List.of());
            ServersGameTest.m().worldSort.set(WorldSort.CUSTOM);
        });
        open();
        check(ours().equals(List.of(A, B, C)), "new worlds start by last played: " + ours());
        scrollTo(A);
        double[] from = grip(C), to = grip(A);
        ServerListsKit.drag(ctx, from[0], from[1], to[0], to[2], () -> SHOTS.shot(ctx, "worlds_dragging"));
        check(ours().equals(List.of(C, A, B)), "world C dragged to the top: " + ours());
        check(stored().equals(List.of(C, A, B)), "custom order stored: " + ctx.computeOnClient(mc -> WorldOrderStore.get()));
        SHOTS.shot(ctx, "worlds_custom");
        ctx.setScreen(TitleScreen::new);
        ctx.runOnClient(mc -> WorldOrderStore.reload());
        open();
        check(ours().equals(List.of(C, A, B)), "custom order survives reopening: " + ours());
        clickSort();
        check(ctx.computeOnClient(mc -> ServersGameTest.m().worldSort.get()) == WorldSort.LAST_PLAYED, "chip cycles to last played");
        check(ours().equals(List.of(A, B, C)), "last played order: " + ours());
        SHOTS.shot(ctx, "worlds_last_played");
        clickSort();
        clickSort();
        check(ours().equals(List.of(C, A, B)), "back to custom: " + ours());
        ctx.setScreen(TitleScreen::new);
    }

    private List<String> stored() {
        List<String> out = new ArrayList<>();
        for (String id : ctx.computeOnClient(mc -> WorldOrderStore.get())) if (id.startsWith("vibey_dnd_")) out.add(id);
        return out;
    }

    private void open() {
        ctx.setScreen(() -> new SelectWorldScreen(new TitleScreen()));
        ctx.waitFor(mc -> oursHere(mc).size() == 3, 200);
        ctx.waitTicks(5);
    }

    private void clickSort() {
        double[] c = ctx.computeOnClient(mc -> {
            var e = WorldScreenHooks.current().sortView().document().find("sort");
            var r = dev.henny.vui.layout.ScreenRects.of(e);
            return new double[] {r.centerX(), r.centerY()};
        });
        ServerListsKit.click(ctx, c[0], c[1]);
        ctx.waitTicks(3);
    }

    private void scrollTo(String id) {
        ctx.runOnClient(mc -> {
            WorldSelectionList list = list(mc);
            for (WorldSelectionList.Entry e : list.children()) {
                if (e.getLevelSummary() != null && e.getLevelSummary().getLevelId().equals(id)) {
                    list.setScrollAmount(list.scrollAmount() + e.getY() - list.getY());
                    return;
                }
            }
        });
        ctx.waitTicks(3);
    }

    private double[] grip(String id) {
        return ctx.computeOnClient(mc -> {
            for (WorldSelectionList.Entry e : list(mc).children()) {
                if (e.getLevelSummary() != null && e.getLevelSummary().getLevelId().equals(id)) {
                    return new double[] {e.getContentX() + 160, e.getContentY() + 16, e.getContentY() + 3};
                }
            }
            throw new AssertionError("no world row " + id);
        });
    }

    private List<String> ours() {
        return ctx.computeOnClient(WorldOrderChecks::oursHere);
    }

    private static List<String> oursHere(Minecraft mc) {
        List<String> out = new ArrayList<>();
        WorldSelectionList list = list(mc);
        if (list == null) return out;
        for (WorldSelectionList.Entry e : list.children()) {
            if (e.getLevelSummary() != null && e.getLevelSummary().getLevelId().startsWith("vibey_dnd_")) out.add(e.getLevelSummary().getLevelId());
        }
        return out;
    }

    private static WorldSelectionList list(Minecraft mc) {
        return mc.screen instanceof SelectWorldScreen s ? ((SelectWorldAccess) s).vibey$list() : null;
    }

    private void makeWorlds() {
        boolean have = ctx.computeOnClient(mc -> WorldFixtures.exists(mc, A));
        if (!have) {
            try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
                world.getClientWorld().waitForChunksRender();
            }
        }
        long hour = 3_600_000L, now = System.currentTimeMillis();
        ctx.runOnClient(mc -> {
            WorldFixtures.copyFirst(mc, A, "Alpine Base", now - hour);
            WorldFixtures.copyFirst(mc, B, "Bamboo Isle", now - 26 * hour);
            WorldFixtures.copyFirst(mc, C, "Coral Reef", now - 72 * hour);
        });
        log("worlds ready");
    }
}
