package dev.henny.vibeyclient.gametest;

import dev.henny.r3.mc.R3Mc;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.info.card.CardData;
import dev.henny.vibeyclient.module.info.card.CardState;
import dev.henny.vibeyclient.module.info.core.HarvestVerdict;
import dev.henny.vibeyclient.module.info.pin.PinnedCard;
import dev.henny.vibeyclient.module.info.pin.PinnedCards;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.joml.Vector3d;

import static dev.henny.vibeyclient.gametest.InfoTestKit.MIDDLE;
import static dev.henny.vibeyclient.gametest.InfoTestKit.check;
import static dev.henny.vibeyclient.gametest.InfoTestKit.cmd;
import static dev.henny.vibeyclient.gametest.InfoTestKit.hold;
import static dev.henny.vibeyclient.gametest.InfoTestKit.log;
import static dev.henny.vibeyclient.gametest.InfoTestKit.lookAt;
import static dev.henny.vibeyclient.gametest.InfoTestKit.shot;
import static dev.henny.vibeyclient.gametest.InfoTestKit.turn;

final class InfoCardChecks {
    private InfoCardChecks() {}

    static void blocks(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        Vector3d ore = new Vector3d(o[0] - 0.5, o[1] + 0.5, o[2] - 1.02);
        harvest(ctx, world, ore, "minecraft:iron_pickaxe", HarvestVerdict.OK, "card_ore_iron_pickaxe");
        harvest(ctx, world, ore, "minecraft:stone_pickaxe", HarvestVerdict.TOO_WEAK, "card_ore_stone_pickaxe");
        harvest(ctx, world, ore, "minecraft:air", HarvestVerdict.WRONG_TOOL, "card_ore_hand");
        crop(ctx, new Vector3d(o[0] + 1.5, o[1] + 0.2, o[2] - 1.6), 43, "card_wheat_growing");
        crop(ctx, new Vector3d(o[0] + 2.5, o[1] + 0.4, o[2] - 1.6), 100, "card_wheat_mature");
    }

    private static void harvest(ClientGameTestContext ctx, TestSingleplayerContext world, Vector3d at, String item,
                                HarvestVerdict expected, String name) {
        cmd(world, "item replace entity @a weapon.mainhand with " + item);
        ctx.waitTicks(3);
        lookAt(ctx, at);
        CardData d = ctx.computeOnClient(mc -> CardState.current());
        check(d != null && d.harvest() != null, "card should show for the ore");
        log("%s: %s %s %s", name, d.title(), d.mod(), d.harvest());
        check("Diamond Ore".equals(d.title()) && "Minecraft".equals(d.mod()), "ore card title/mod: " + d.title());
        check(d.harvest().verdict() == expected, name + " verdict " + d.harvest().verdict());
        shot(ctx, name);
    }

    private static void crop(ClientGameTestContext ctx, Vector3d at, int growth, String name) {
        lookAt(ctx, at);
        CardData d = ctx.computeOnClient(mc -> CardState.current());
        check(d != null && d.growth() == growth, name + " growth " + (d == null ? null : d.growth()));
        shot(ctx, name);
    }

    static void mob(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        cmd(world, "summon minecraft:cow %.1f %d %.1f {NoAI:1b,Rotation:[0f,0f]}", o[0] + 4.5, o[1], o[2] + 0.5);
        ctx.waitTicks(6);
        lookAt(ctx, new Vector3d(o[0] + 4.5, o[1] + 0.8, o[2] + 0.5));
        CardData d = ctx.computeOnClient(mc -> CardState.current());
        check(d != null && d.entity() != null && "Cow".equals(d.title()), "cow card: " + (d == null ? null : d.title()));
        check(d.health() == null, "a cow's health is not visible in vanilla");
        shot(ctx, "card_cow");
    }

    static void pin(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        Vector3d wheat = new Vector3d(o[0] + 1.5, o[1] + 0.2, o[2] - 1.6);
        lookAt(ctx, wheat);
        hold(ctx, Keys.I, 3);
        check(count(ctx) == 0, "a short press must not pin");
        hold(ctx, Keys.I, 12);
        check(count(ctx) == 1, "holding the pin key should pin the card");
        Vector3d pinned = ctx.computeOnClient(mc -> new Vector3d(first().panel().transform().position()));
        lookAt(ctx, new Vector3d(pinned).add(-1.2, -0.9, 0));
        String title = ctx.computeOnClient(mc -> PinnedCards.all().get(0).controller().title.peek());
        check("Wheat Crops".equals(title), "pinned card title " + title);
        shot(ctx, "pin_card_in_world");

        Vector3d center = ctx.computeOnClient(mc -> new Vector3d(first().panel().transform().position()));
        lookAt(ctx, center);
        check(ctx.computeOnClient(mc -> PinnedCards.hovered()) != null, "crosshair on the card should hover it");
        check(ctx.computeOnClient(mc -> CardState.current()) == null, "HUD card hides while looking at a pinned card");
        shot(ctx, "pin_card_hovered");
        ctx.getInput().holdMouse(MIDDLE);
        ctx.waitTicks(4);
        Vector3d target = new Vector3d(o[0] - 2.5, o[1] + 1.5, o[2] - 3.5);
        turn(ctx, center, target, 6);
        ctx.getInput().releaseMouse(MIDDLE);
        ctx.waitTicks(4);
        Vector3d moved = ctx.computeOnClient(mc -> new Vector3d(first().panel().transform().position()));
        log("pinned card moved %.2f blocks", moved.distance(center));
        check(moved.distance(center) > 0.8, "middle-drag should move the pinned card");
        shot(ctx, "pin_card_moved");

        lookAt(ctx, moved);
        float before = ctx.computeOnClient(mc -> first().panel().transform().worldWidth());
        ctx.getInput().scroll(1);
        ctx.waitTicks(4);
        float after = ctx.computeOnClient(mc -> first().panel().transform().worldWidth());
        check(after > before, "wheel should grow the pinned card");
        shot(ctx, "pin_card_bigger");
        hold(ctx, Keys.I, 2);
        check(count(ctx) == 0, "tapping the pin key on a pinned card should remove it");

        lookAt(ctx, new Vector3d(o[0] + 4.5, o[1] + 0.8, o[2] + 0.5));
        hold(ctx, Keys.I, 12);
        check(count(ctx) == 1 && first().entity() != null, "a mob card should pin too");
        lookAt(ctx, new Vector3d(o[0] + 4.5, o[1] + 1.9, o[2] + 0.5));
        shot(ctx, "pin_mob_card");
        ctx.runOnClient(mc -> PinnedCards.clear());
        check(ctx.computeOnClient(mc -> R3Mc.scene().objects().stream().noneMatch(w -> w.kind().equals(R3Mc.PANEL_KIND))),
                "no panels left after clear");
    }

    private static int count(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> PinnedCards.all().size());
    }

    private static PinnedCard first() {
        return PinnedCards.all().get(0);
    }
}
