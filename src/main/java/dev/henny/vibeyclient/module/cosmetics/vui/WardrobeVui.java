package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.cosmetico.preview.MouseAction;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi;
import dev.henny.vibeyclient.ui.vui.HostGfx;
import dev.henny.vibeyclient.ui.vui.SyncedScreen;
import dev.henny.vibeyclient.ui.vui.VibeyVui;
import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.LayoutBox;
import net.minecraft.client.gui.screens.Screen;

public final class WardrobeVui extends SyncedScreen {
    public static final String DOC = "vibeyclient:wardrobe/wardrobe";
    private static String lastSlot = "cape";

    private final WardrobeController controller;
    private boolean dragging;
    private Object graphics;
    private float delta;

    private WardrobeVui(Screen parent, WardrobeController controller) {
        super(DOC, controller, parent);
        this.controller = controller;
        background(!ClientCompat.inWorld());
    }

    public static void open(Screen parent, String slotId) {
        VibeyVui.init();
        String slot = slotId == null ? lastSlot : slotId;
        WardrobeController c = new WardrobeController(slot, () -> ClientCompat.openScreen(parent),
                () -> WardrobeUi.openWheelEditor(ClientCompat.screen()));
        ClientCompat.openScreen(new WardrobeVui(parent, c));
    }

    public WardrobeController controller() {
        return controller;
    }

    @Override
    protected void sync() {
        lastSlot = controller.slot.peek();
        controller.sync();
    }

    @Override
    protected void init() {
        super.init();
        document().beforeOverlays(c -> drawStage());
    }

    @Override
    protected void draw(Object graphics, int mouseX, int mouseY, float delta) {
        this.graphics = graphics;
        this.delta = delta;
        super.draw(graphics, mouseX, mouseY, delta);
        this.graphics = null;
    }

    private void drawStage() {
        LayoutBox b = stageBox();
        if (graphics == null || b == null || b.width <= 4 || b.height <= 4) return;
        controller.stage.draw(HostGfx.of(graphics), Math.round(b.x) + 1, Math.round(b.y) + 1, Math.round(b.width) - 2,
                Math.round(b.height) - 2, delta);
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        boolean used = super.pointerDown(x, y, button);
        LayoutBox b = stageBox();
        if (button == 0 && b != null && inside(b, x, y) && controller.stage.mouse(MouseAction.PRESS, x, y)) dragging = true;
        return used || dragging;
    }

    @Override
    protected boolean pointerMove(double x, double y) {
        if (dragging) controller.stage.mouse(MouseAction.DRAG, x, y);
        return super.pointerMove(x, y) || dragging;
    }

    @Override
    protected boolean pointerUp(double x, double y, int button) {
        if (dragging && button == 0) {
            dragging = false;
            controller.stage.mouse(MouseAction.RELEASE, x, y);
        }
        return super.pointerUp(x, y, button);
    }

    @Override
    public void removed() {
        controller.stage.reset();
        super.removed();
    }

    private LayoutBox stageBox() {
        Element e = document().ref("stage");
        return e == null ? null : e.box();
    }

    private static boolean inside(LayoutBox b, double x, double y) {
        return x >= b.x && y >= b.y && x < b.x + b.width && y < b.y + b.height;
    }
}
