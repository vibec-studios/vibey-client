package dev.henny.vibeyclient.ui.vui;

import dev.henny.vibeyclient.ui.UiScale;
import dev.henny.vui.mc.host.VuiScreen;
import net.minecraft.client.gui.screens.Screen;

public abstract class SyncedScreen extends VuiScreen {
    private static final int SYNC_TICKS = 5;

    private final Screen back;
    private int ticks;

    protected SyncedScreen(String documentId, Object controller, Screen parent) {
        super(documentId, controller);
        this.back = parent;
        parent(parent);
    }

    public Screen back() {
        return back;
    }

    protected abstract void sync();

    @Override
    protected void init() {
        UiScale.apply(this);
        super.init();
    }

    @Override
    public void removed() {
        super.removed();
        UiScale.restore();
    }

    @Override
    public boolean isPauseScreen() {
        return back != null && back.isPauseScreen();
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks % SYNC_TICKS != 0) return;
        sync();
        document().refresh();
    }
}
