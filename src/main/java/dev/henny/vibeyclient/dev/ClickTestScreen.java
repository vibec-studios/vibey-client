package dev.henny.vibeyclient.dev;

import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.UiHost;
import dev.henny.vibeyclient.ui.VibeyButton;
import dev.henny.vibeyclient.ui.Widgets;
import net.minecraft.network.chat.Component;

/**
 * Target screen of {@link ClickTest}: an immediate-mode {@link Widgets#button} (left click), a right-click area and a
 * real {@link VibeyButton} widget, each counting its clicks, all through {@link BaseScreen}'s input glue.
 */
final class ClickTestScreen extends BaseScreen {
    static final int BW = 140, BH = 20;

    private final Ui ui = UiHost.create(this::addWidget);
    private VibeyButton vibey;
    int leftClicks, rightClicks, vibeyClicks;

    ClickTestScreen() {
        super(Component.literal("Vibey click test"));
    }

    int col() {
        return width / 2 - BW / 2;
    }

    int uiButtonY() {
        return height / 2 - 50;
    }

    int rightAreaY() {
        return height / 2 - 20;
    }

    int vibeyButtonY() {
        return height / 2 + 10;
    }

    @Override
    protected void init() {
        vibey = new VibeyButton(col(), vibeyButtonY(), BW, BH, Component.literal("VibeyButton (left)"), null, () -> vibeyClicks++);
        addRenderableWidget(vibey);
        UiHost.reattach(ui);
    }

    @Override
    protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
        UiHost.begin(ui, g, mouseX, mouseY, delta);
        g.rect(0, 0, width, height, Theme.SURFACE);
        g.textCenter("Vibey click test", width / 2, uiButtonY() - 30, Theme.TEXT);
        int x = col();
        if (Widgets.button(ui, "left", x, uiButtonY(), BW, BH, "Ui button (left) x" + leftClicks, Widgets.Style.PRIMARY)) leftClicks++;
        boolean rightHit = ui.clicked(x, rightAreaY(), BW, BH, Keys.MOUSE_RIGHT);
        if (rightHit) rightClicks++;
        g.rect(x, rightAreaY(), BW, BH, Theme.SURFACE3);
        g.outline(x, rightAreaY(), BW, BH, Theme.LINE2);
        g.textCenter("Right-click area x" + rightClicks, width / 2, rightAreaY() + 6, Theme.TEXT);
        g.widget(vibey, mouseX, mouseY, delta);
        g.textCenter("VibeyButton clicks: " + vibeyClicks, width / 2, vibeyButtonY() + BH + 8, Theme.MUTED);
        UiHost.end(ui);
    }

    @Override
    protected boolean click(double x, double y, int button) {
        return UiHost.click(ui, x, y, button);
    }

    @Override
    protected boolean release(double x, double y, int button) {
        UiHost.release(ui);
        return false;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        return false;
    }
}
