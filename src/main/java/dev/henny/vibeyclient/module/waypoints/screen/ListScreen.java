package dev.henny.vibeyclient.module.waypoints.screen;

import dev.henny.vui.mc.host.VuiScreen;

public final class ListScreen extends VuiScreen {
    public static final String DOCUMENT = "vibeyclient:waypoints/list";

    private final ListController controller;

    ListScreen(ListController controller) {
        super(DOCUMENT, controller);
        this.controller = controller;
    }

    public ListController controller() {
        return controller;
    }

    @Override
    protected void draw(Object graphics, int mouseX, int mouseY, float delta) {
        controller.poll();
        super.draw(graphics, mouseX, mouseY, delta);
    }
}
