package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.clips.shots.GalleryActions;
import dev.henny.vibeyclient.module.clips.shots.ShotFiles;
import dev.henny.vibeyclient.module.clips.shots.ShotRow;
import dev.henny.vibeyclient.module.clips.shots.Shots;
import dev.henny.vibeyclient.module.clips.shots.ShotsScreen;
import dev.henny.vibeyclient.module.clips.shots.core.Shot;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.henny.vibeyclient.gametest.GalleryAim.clickScreen;
import static dev.henny.vibeyclient.gametest.GalleryGameTest.KIT;
import static dev.henny.vibeyclient.gametest.GalleryGameTest.c;
import static dev.henny.vibeyclient.gametest.GalleryGameTest.selected;
import static dev.henny.vibeyclient.gametest.GalleryGameTest.waitFor;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;

final class GalleryScreenChecks {
    private GalleryScreenChecks() {}

    static void screen(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> Shots.openScreen());
        waitFor(ctx, "gallery rows", 200, () -> c().rows.size() >= GalleryFixtures.COUNT);
        waitFor(ctx, "thumbnails", 400, () -> c().rows.snapshot().stream().allMatch(r -> !r.thumb.peek().isEmpty()));
        ctx.waitTicks(5);
        int n = ctx.computeOnClient(mc -> c().rows.size());
        KIT.log("gallery shows %d screenshots, %d textures", n, ctx.computeOnClient(mc -> c().loadedTextures()));
        check(ctx.computeOnClient(mc -> mc.screen instanceof ShotsScreen), "gallery screen should be open");
        check(sorted(ctx, true), "newest first by default");
        KIT.shot(ctx, "screen_newest");
        clickScreen(ctx, ".shot-tile", 2);
        String third = ctx.computeOnClient(mc -> c().rows.get(2).id);
        check(third.equals(ctx.computeOnClient(mc -> selected())), "clicking a tile selects it");
        waitFor(ctx, "preview", 200, () -> !c().preview.peek().isEmpty());
        KIT.shot(ctx, "screen_selected");
        clickScreen(ctx, "#next", 0);
        String fourth = ctx.computeOnClient(mc -> c().rows.get(3).id);
        check(fourth.equals(ctx.computeOnClient(mc -> selected())), "next steps to the following screenshot");
        clickScreen(ctx, "#sort", 0);
        check("oldest".equals(ctx.computeOnClient(mc -> c().sort.peek())) && sorted(ctx, false), "sort chip: oldest first");
        KIT.shot(ctx, "screen_oldest");
        clickScreen(ctx, "#sort", 0);
        clickScreen(ctx, "#sort", 0);
        check("newest".equals(ctx.computeOnClient(mc -> c().sort.peek())), "sort cycles back to newest");
        favourite(ctx);
        copy(ctx);
        delete(ctx);
    }

    private static void favourite(ClientGameTestContext ctx) {
        String sel = ctx.computeOnClient(mc -> selected());
        clickScreen(ctx, "#fav", 0);
        ctx.waitTicks(10);
        check(favouritesFile().contains(sel), "favourite saved to vibeyclient/screenshots.json");
        clickScreen(ctx, "#favs", 0);
        check(ctx.computeOnClient(mc -> c().rows.size()) == 1, "favourites filter shows only the favourite");
        KIT.shot(ctx, "screen_favourites_only");
        clickScreen(ctx, "#favs", 0);
        check(ctx.computeOnClient(mc -> c().rows.size()) > 1, "filter off shows all again");
    }

    private static void copy(ClientGameTestContext ctx) {
        clickScreen(ctx, "#copy", 0);
        waitFor(ctx, "copy", 400, () -> !GalleryActions.lastCopy().isEmpty());
        KIT.log("copy: %s", GalleryActions.lastCopy());
        ctx.waitTicks(3);
        KIT.shot(ctx, "screen_copied_toast");
    }

    private static void delete(ClientGameTestContext ctx) {
        String sel = ctx.computeOnClient(mc -> selected());
        int before = ctx.computeOnClient(mc -> c().rows.size());
        Path file = ShotFiles.dir().resolve(sel);
        clickScreen(ctx, "#delete", 0);
        check(ctx.computeOnClient(mc -> c().armed.peek()) && Files.exists(file), "first delete click only arms");
        KIT.shot(ctx, "screen_delete_armed");
        clickScreen(ctx, "#delete", 0);
        waitFor(ctx, "deleted", 200, () -> !Files.exists(file) && c().rows.snapshot().stream().noneMatch(r -> r.id.equals(sel)));
        KIT.log("delete: %s gone, %d -> %d rows (review shots land in screenshots/ too)", sel, before,
                ctx.computeOnClient(mc -> c().rows.size()));
        check(ctx.computeOnClient(mc -> selected()) != null, "a neighbour is selected after delete");
        KIT.shot(ctx, "screen_after_delete");
    }

    private static boolean sorted(ClientGameTestContext ctx, boolean newest) {
        List<Shot> shown = ctx.computeOnClient(mc -> new ArrayList<>(c().shown()));
        for (int i = 1; i < shown.size(); i++) {
            long a = shown.get(i - 1).modifiedMs(), b = shown.get(i).modifiedMs();
            if (newest ? a < b : a > b) return false;
        }
        return true;
    }

    private static String favouritesFile() {
        try {
            return Files.readString(FabricLoader.getInstance().getGameDir().resolve("vibeyclient/screenshots.json"));
        } catch (IOException e) {
            return "";
        }
    }

    static ShotRow row(int i) {
        return c().rows.get(i);
    }
}
