package dev.henny.vibeyclient.module.clips.shots;

import dev.henny.vui.mc.host.VuiScreen;

public final class ShotsScreen extends VuiScreen {
    public static final String DOCUMENT = "vibeyclient:clips/shots";

    private final GalleryController controller;

    ShotsScreen(GalleryController controller) {
        super(DOCUMENT, controller);
        this.controller = controller;
    }

    public GalleryController controller() {
        return controller;
    }

    @Override
    protected void draw(Object graphics, int mouseX, int mouseY, float delta) {
        controller.poll();
        super.draw(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        super.removed();
        Shots.screenClosed(this);
    }
}
