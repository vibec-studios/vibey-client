package dev.henny.vibeyclient.avatar;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.avatar.preview.AvatarPreview;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Dev-only runtime check for versions without a client gametest: {@code -Dvibeyclient.avatarTest=true} or env {@code VIBEYCLIENT_AVATAR_TEST=true} (development
 * environment only). On the title screen it equips a few items, shows three previews (front/side/back) playing an
 * emote and takes screenshots; in a world (e.g. started with {@code --quickPlaySingleplayer}) it wears items and
 * plays an emote in third person; then it quits. Logs lines starting with {@code [vibeyclient-avatartest]}.
 */
public final class SelfTest {
    private SelfTest() {}

    private static final boolean ON = Vibey.platform().isDevelopment()
            && (Boolean.getBoolean("vibeyclient.avatarTest") || "true".equals(System.getenv("VIBEYCLIENT_AVATAR_TEST")));
    private static int step = -1;
    private static int wait;
    private static int settle;

    public static boolean enabled() {
        return ON;
    }

    /** Every client tick (cosmetics module). */
    public static void tick() {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (wait > 0) {
            wait--;
            return;
        }
        Wardrobe w = Services.get(Wardrobe.class).orElse(null);
        switch (step) {
            case -1 -> {
                if (AvatarItems.all().size() < 20 || w == null || ClientCompat.screen() == null || loading(mc)) return;
                if (settle++ < 120) return;
                log("items " + AvatarItems.all().size() + ", Minecraft " + dev.henny.vibeyclient.BuildInfo.MINECRAFT);
                for (String id : new String[]{"vibey:vibe_cape", "vibey:crystal_wings", "vibey:seal_beanie", "vibey:seal_buddy"}) w.equip(AvatarItems.get(id));
                ClientCompat.openScreen(new TestScreen());
                step = 0;
                wait = 40;
            }
            case 0 -> {
                shot("avatartest_title_previews");
                if (ClientCompat.screen() instanceof TestScreen t) for (AvatarPreview.View v : t.views) v.playEmote("vibey:wave");
                step = 1;
                wait = 18;
            }
            case 1 -> {
                shot("avatartest_title_wave");
                ClientCompat.openScreen(null);
                step = 2;
                wait = 40;
            }
            case 2 -> {
                if (mc.player == null) {
                    // no world: done
                    finish();
                    return;
                }

                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                step = 3;
                wait = 30;
            }
            case 3 -> {
                shot("avatartest_world_front");
                if (w != null) w.playEmote("vibey:guitar");
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                step = 4;
                wait = 14;
            }
            case 4 -> {
                shot("avatartest_world_guitar");
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                step = 5;
                wait = 10;
            }
            case 5 -> {
                shot("avatartest_world_back");
                finish();
            }
            default -> {}
        }
    }

    private static boolean loading(Minecraft mc) {
        //? if >=26.3 {
        /*return false;
        *///?} else
        return mc.getOverlay() != null;
    }

    private static void finish() {
        step = 99;
        log("done");
        Minecraft mc = Minecraft.getInstance();
        Wardrobe w = Services.get(Wardrobe.class).orElse(null);
        if (w != null) for (Slot s : Slot.all()) w.unequip(s);
        dev.henny.vibeyclient.api.Tasks.later(1500, () -> dev.henny.vibeyclient.api.Tasks.onMain(mc::stop));
    }

    private static void log(String s) {
        Vibey.LOG.info("[vibeyclient-avatartest] {}", s);
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        try {
            //? if >=26.3 {
            /*net.minecraft.client.Screenshot.grab(mc, false);
            *///?} else if >=1.21.6 {
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, c -> log("screenshot " + c.getString()));
            //?} else
            /*net.minecraft.client.Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> log("screenshot " + c.getString()));*/
            log("shot " + name);
        } catch (RuntimeException e) {
            log("screenshot failed: " + e);
        }
    }

    private static final class TestScreen extends BaseScreen {
        final AvatarPreview.View[] views = new AvatarPreview.View[3];

        TestScreen() {
            super(Component.literal("Avatar test"));
            AvatarPreview p = Services.get(AvatarPreview.class).orElseThrow();
            for (int i = 0; i < 3; i++) {
                views[i] = p.create();
                views[i].setYaw(i * 90f);
            }
        }

        @Override
        protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
            g.rect(0, 0, width, height, Theme.BG);
            int w = width / 3;
            for (int i = 0; i < 3; i++) {
                g.rect(i * w + 8, 8, w - 16, height - 16, Theme.SURFACE2);
                views[i].draw(g, i * w + 8, 8, w - 16, height - 16, delta);
            }
        }

        @Override
        protected boolean click(double x, double y, int button) {
            return false;
        }

        @Override
        protected boolean scroll(double x, double y, double amount) {
            return false;
        }
    }
}
