package dev.henny.vibeyclient.module.inventory.screen;

import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.inventory.InventoryModule;
import dev.henny.vibeyclient.module.inventory.click.ClickSender;
import dev.henny.vibeyclient.module.inventory.mixin.ContainerScreenAccess;
import dev.henny.vibeyclient.module.inventory.search.SearchOverlay;
import dev.henny.vibeyclient.module.inventory.search.SearchState;
import dev.henny.vui.mc.input.KeyCodes;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class ContainerSession {
    private final InventoryModule m;
    private final AbstractContainerScreen<?> screen;
    private final TweakInput tweaks;
    private final SearchOverlay search;

    ContainerSession(InventoryModule m, AbstractContainerScreen<?> screen) {
        this.m = m;
        this.screen = screen;
        this.tweaks = new TweakInput(m);
        if (!m.searchKeep.on()) SearchState.clear();
        this.search = m.searchActive() ? new SearchOverlay(SearchState.raw()) : null;
    }

    public SearchOverlay search() {
        return search;
    }

    public boolean dragging() {
        return tweaks.dragging();
    }

    boolean searching() {
        return search != null && m.searchActive();
    }

    void render(Object graphics, int mouseX, int mouseY) {
        ClickSender.pump();
        tweaks.frame(screen);
        if (!searching()) return;
        SearchState.refresh(screen.getMenu());
        ContainerScreenAccess a = (ContainerScreenAccess) screen;
        float x = a.vibey$leftPos() + a.vibey$imageWidth() - SearchOverlay.WIDTH;
        float y = Math.max(2, a.vibey$topPos() - SearchOverlay.HEIGHT - 2);
        search.draw(graphics, mouseX, mouseY, screen.width, screen.height, x, y);
    }

    boolean click(double x, double y, int button, boolean shift) {
        if (searching() && search.down(x, y, button)) return false;
        if (ClickSender.midPlan()) return false;
        tweaks.press(screen, button, shift);
        return true;
    }

    boolean release(double x, double y, int button) {
        tweaks.release();
        return !(searching() && search.up(x, y, button));
    }

    boolean scroll(double x, double y, double dy) {
        if (searching() && search.hits(x, y)) return false;
        return !tweaks.scroll(screen, dy);
    }

    boolean key(int key, int scancode, boolean ctrl) {
        if (!searching()) return true;
        if (search.focused()) {
            if (key == Keys.ESCAPE) search.blur();
            else search.key(KeyCodes.fromNative(key), scancode);
            return false;
        }
        if (ctrl && key == Keys.F) {
            search.focus();
            return false;
        }
        return true;
    }

    boolean typed(int codepoint) {
        return searching() && search.typed(codepoint);
    }

    void close() {
        tweaks.stop();
        if (search != null) search.close();
        if (!m.searchKeep.on()) SearchState.clear();
    }
}
