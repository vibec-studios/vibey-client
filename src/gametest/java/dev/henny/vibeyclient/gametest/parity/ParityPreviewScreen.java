package dev.henny.vibeyclient.gametest.parity;

import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.network.chat.Component;

public final class ParityPreviewScreen extends BaseScreen {
    public interface View {
        void draw(Gfx g, int x, int y, int w, int h, float delta);
    }

    final View[] views;

    ParityPreviewScreen(View[] views) {
        super(Component.literal("Parity previews"));
        this.views = views;
    }

    @Override
    protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
        g.rect(0, 0, width, height, Theme.BG);
        int w = width / views.length;
        for (int i = 0; i < views.length; i++) {
            g.rect(i * w + 8, 8, w - 16, height - 16, Theme.SURFACE2);
            views[i].draw(g, i * w + 8, 8, w - 16, height - 16, 0f);
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
