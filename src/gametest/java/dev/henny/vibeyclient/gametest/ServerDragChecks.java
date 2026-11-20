package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.client.Keys;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

import java.util.List;

import static dev.henny.vibeyclient.gametest.ServerListsKit.SHOTS;
import static dev.henny.vibeyclient.gametest.ServerListsKit.check;
import static dev.henny.vibeyclient.gametest.ServerListsKit.log;
import static dev.henny.vibeyclient.gametest.ServerListsKit.saved;
import static dev.henny.vibeyclient.gametest.ServerListsKit.session;

final class ServerDragChecks {
    static final String PREFIX = "DnD ";
    static final String ONE = "DnD One", TWO = "DnD Two", THREE = "DnD Three", FOUR = "DnD Four";

    private final ClientGameTestContext ctx;

    ServerDragChecks(ClientGameTestContext ctx) {
        this.ctx = ctx;
    }

    private double[] grip(String name) {
        return ctx.computeOnClient(mc -> {
            ServerSelectionList.OnlineServerEntry e = ServersTestKit.entry(name);
            check(e != null, "no row " + name);
            return new double[] {e.getContentX() + 150, e.getContentY() + 16, e.getContentY() + 3};
        });
    }

    void run() {
        check(saved(ctx, PREFIX).equals(List.of(ONE, TWO, THREE, FOUR)), "start order " + saved(ctx, PREFIX));
        dragThreeToTop();
        escCancels();
        clickStillSelects();
        filteredDrag();
    }

    private void dragThreeToTop() {
        double[] from = grip(THREE), to = grip(ONE);
        SHOTS.shot(ctx, "servers_before");
        ServerListsKit.drag(ctx, from[0], from[1], to[0], to[2], () -> {
            check(ctx.computeOnClient(mc -> session().drag().dragging()), "row should be dragging");
            SHOTS.shot(ctx, "servers_dragging");
        });
        List<String> order = saved(ctx, PREFIX);
        log("after drag %s", order);
        check(order.equals(List.of(THREE, ONE, TWO, FOUR)), "server 3 should be first and saved: " + order);
        check(ServersTestKit.online(ctx).subList(0, 4).equals(order), "list shows the saved order " + ServersTestKit.online(ctx));
        String selected = ctx.computeOnClient(mc -> ServersTestKit.list().getSelected() instanceof ServerSelectionList.OnlineServerEntry o
                ? o.getServerData().name : "");
        check(THREE.equals(selected), "dragged row stays selected: " + selected);
        SHOTS.shot(ctx, "servers_dropped");
    }

    private void escCancels() {
        double[] from = grip(FOUR), to = grip(THREE);
        ServerListsKit.drag(ctx, from[0], from[1], to[0], to[2], () -> {
            ctx.getInput().pressKey(Keys.toNative(Keys.ESCAPE));
            ctx.waitTicks(2);
        });
        check(saved(ctx, PREFIX).equals(List.of(THREE, ONE, TWO, FOUR)), "Esc cancels the drag: " + saved(ctx, PREFIX));
        check(ctx.computeOnClient(mc -> ServerListsKit.session() != null), "Esc during a drag keeps the screen open");
    }

    private void clickStillSelects() {
        double[] at = grip(TWO);
        ServerListsKit.click(ctx, at[0], at[1]);
        String selected = ctx.computeOnClient(mc -> ServersTestKit.list().getSelected() instanceof ServerSelectionList.OnlineServerEntry o
                ? o.getServerData().name : "");
        check(TWO.equals(selected), "a plain click selects: " + selected);
        check(saved(ctx, PREFIX).equals(List.of(THREE, ONE, TWO, FOUR)), "a click does not move");
    }

    private void filteredDrag() {
        ctx.runOnClient(mc -> session().bar().query.set("r"));
        ctx.waitTicks(4);
        check(ServersTestKit.online(ctx).equals(List.of(THREE, FOUR)), "filter 'r' " + ServersTestKit.online(ctx));
        double[] from = grip(FOUR), to = grip(THREE);
        ServerListsKit.drag(ctx, from[0], from[1], to[0], to[2], null);
        List<String> order = saved(ctx, PREFIX);
        check(order.equals(List.of(FOUR, THREE, ONE, TWO)), "filtered drag maps to the real list: " + order);
        SHOTS.shot(ctx, "servers_filtered_drop");
        ctx.runOnClient(mc -> session().bar().clear());
        ctx.waitTicks(3);
    }
}
