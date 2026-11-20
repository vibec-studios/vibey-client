package dev.henny.vibeyclient.gametest.parity;

import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.network.chat.Component;

public final class ParityThumbScreen extends BaseScreen {
    public interface Thumb {
        boolean draw(Gfx g, int index, int x, int y, int size);
    }

    private static final int SIZE = 96;
    private final int count;
    private final Thumb thumb;

    public ParityThumbScreen(int count, Thumb thumb) {
        super(Component.literal("Parity thumbnails"));
        this.count = count;
        this.thumb = thumb;
    }

    @Override
    protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
        g.rect(0, 0, width, height, Theme.BG);
        for (int i = 0; i < count; i++) {
            int x = 16 + (i % 4) * (SIZE + 16);
            int y = 16 + (i / 4) * (SIZE + 16);
            g.rect(x, y, SIZE, SIZE, Theme.SURFACE2);
            thumb.draw(g, i, x, y, SIZE);
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
