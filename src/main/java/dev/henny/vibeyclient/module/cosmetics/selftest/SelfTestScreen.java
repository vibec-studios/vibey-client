package dev.henny.vibeyclient.module.cosmetics.selftest;

import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.module.cosmetics.preview.AvatarPreview;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.network.chat.Component;

final class SelfTestScreen extends BaseScreen {
    final AvatarPreview.View[] views = new AvatarPreview.View[3];

    SelfTestScreen() {
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
