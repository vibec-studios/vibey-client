package dev.henny.vibeyclient.module.cosmetics.selftest;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.model.Slot;
import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class SelfTest {
    private SelfTest() {}

    private static final String[] ITEMS = {"vibey:vibe_cape", "vibey:crystal_wings", "vibey:seal_beanie", "vibey:seal_buddy"};
    private static final SelfTestDevlink DEVLINK = new SelfTestDevlink();
    private static int step = -1, wait, settle, waited;

    public static boolean enabled() {
        return SelfTestFlags.ON;
    }

    public static void tick() {
        if (!SelfTestFlags.ON) return;
        if (wait > 0) {
            wait--;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Wardrobe w = Services.get(Wardrobe.class).orElse(null);
        switch (step) {
            case -1 -> start(mc, w);
            case 0 -> titlePreviews();
            case 1 -> next("avatartest_title_wave", 2, 40, () -> ClientCompat.openScreen(null));
            case 2 -> enterWorld(mc, w);
            case 3 -> next("avatartest_world_front", 4, 14, () -> {
                if (w != null) w.playEmote("vibey:guitar");
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            });
            case 4 -> next("avatartest_world_guitar", 5, 10, () -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            case 5 -> {
                SelfTestShots.shot("avatartest_world_back");
                finish(w);
            }
            case 50 -> waitForPush(mc, w);
            case 51 -> next("devlink_" + (++DEVLINK.shots) + "_front", 52, 6, () -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            case 52 -> next("devlink_" + DEVLINK.shots + "_back", 50, 0, () -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            default -> {}
        }
    }

    private static void start(Minecraft mc, Wardrobe w) {
        if (Cosmetico.items().all().size() < 20 || w == null || SelfTestWorld.loading(mc)) return;
        if (SelfTestFlags.WORLD && ++waited % 400 == 0) {
            SelfTestFlags.log("still waiting for a world, screen " + (ClientCompat.screen() == null ? "none" : ClientCompat.screen().getClass().getName()));
            SelfTestShots.shot("avatartest_waiting");
        }
        if (SelfTestFlags.WORLD) SelfTestWorld.pressLoad();
        if (SelfTestFlags.WORLD ? mc.player == null || ClientCompat.screen() != null : ClientCompat.screen() == null) return;
        if (settle++ < 120) return;
        SelfTestFlags.log("items " + Cosmetico.items().all().size() + ", Minecraft " + BuildInfo.MINECRAFT_LABEL);
        if (SelfTestFlags.DEVLINK) {
            SelfTestFlags.log("devlink mode: waiting for pushes");
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            go(50, 0);
            return;
        }
        for (String id : ITEMS) w.equip(Cosmetico.items().get(id));
        if (SelfTestFlags.WORLD) {
            go(2, 20);
            return;
        }
        ClientCompat.openScreen(new SelfTestScreen());
        go(0, 40);
    }

    private static void titlePreviews() {
        SelfTestShots.shot("avatartest_title_previews");
        if (ClientCompat.screen() instanceof SelfTestScreen t) for (var v : t.views) v.playEmote("vibey:wave");
        go(1, 18);
    }

    private static void enterWorld(Minecraft mc, Wardrobe w) {
        if (mc.player == null) {
            finish(w);
            return;
        }
        mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        go(3, 30);
    }

    private static void waitForPush(Minecraft mc, Wardrobe w) {
        String id = DEVLINK.newestPush();
        if (id != null) {
            SelfTestFlags.log("dev item pushed: " + id);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            go(51, 25);
        } else if (DEVLINK.idleTooLong()) {
            finish(w);
        }
    }

    private static void next(String shot, int nextStep, int ticks, Runnable then) {
        SelfTestShots.shot(shot);
        then.run();
        go(nextStep, ticks);
    }

    private static void go(int nextStep, int ticks) {
        step = nextStep;
        wait = ticks;
    }

    private static void finish(Wardrobe w) {
        step = 99;
        SelfTestFlags.log("done");
        if (w != null) for (Slot s : Slot.all()) w.unequip(s);
        Minecraft mc = Minecraft.getInstance();
        Tasks.later(1500, () -> Tasks.onMain(mc::stop));
    }
}
