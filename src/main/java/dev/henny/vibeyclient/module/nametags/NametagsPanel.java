package dev.henny.vibeyclient.module.nametags;

import dev.henny.vibeyclient.ui.Panel;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import dev.henny.vibeyclient.ui.screen.SettingsView;

final class NametagsPanel implements Panel {
    private final NametagsModule module;
    private final ScrollArea scroll = new ScrollArea();
    private final MyNameCard mine;

    NametagsPanel(NametagsModule module) {
        this.module = module;
        this.mine = new MyNameCard(module);
    }

    @Override
    public void draw(Ui ui, int x, int y, int w, int h) {
        int pad = Theme.PAD + 4;
        int cx = x + pad, cw = w - pad * 2;
        int cy = scroll.begin(ui, x, y, w, h) + 12;
        cy = ModulesPanel.intro(ui, module, cx, cy, cw);
        cy = mine.draw(ui, cx, cy, cw);
        cy = SettingsView.draw(ui, module.settings(), cx, cy, cw, module.isEnabled());
        scroll.end(ui, cy + pad);
    }
}
