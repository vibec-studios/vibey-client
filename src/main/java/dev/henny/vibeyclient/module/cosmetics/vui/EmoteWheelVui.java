package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi;
import dev.henny.vibeyclient.ui.vui.SyncedScreen;
import dev.henny.vibeyclient.ui.vui.VibeyVui;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.BooleanSupplier;

public final class EmoteWheelVui extends SyncedScreen {
    public static final String DOC = "vibeyclient:wardrobe/wheel";
    static final long TAP_MS = 250;

    private final WheelController controller;
    private final BooleanSupplier keyHeld;
    private final Screen parent;
    private final long openedAt = System.currentTimeMillis();
    private boolean sticky;
    private boolean done;

    private EmoteWheelVui(Screen parent, BooleanSupplier keyHeld, WheelController controller) {
        super(DOC, controller, parent);
        this.parent = parent;
        this.keyHeld = keyHeld;
        this.controller = controller;
        background(false);
    }

    public static void open(Screen parent, BooleanSupplier keyHeld) {
        VibeyVui.init();
        ClientCompat.openScreen(new EmoteWheelVui(parent, keyHeld, new WheelController()));
    }

    public WheelController controller() {
        return controller;
    }

    @Override
    protected void init() {
        super.init();
        controller.layout(width, height);
    }

    @Override
    protected void sync() {
        controller.sync();
    }

    @Override
    public void tick() {
        super.tick();
        if (done || sticky || held()) return;
        if (System.currentTimeMillis() - openedAt < TAP_MS && controller.hovered() < 0) {
            sticky = true;
            controller.sticky(true);
            return;
        }
        finish(true);
    }

    @Override
    protected boolean pointerMove(double x, double y) {
        controller.hover(x, y);
        return true;
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        if (done) return true;
        controller.hover(x, y);
        if (button == 1) {
            finish(false);
        } else if (button == 0) {
            Wardrobe wd = WardrobeController.wardrobe();
            if (controller.hovered() >= 0 && wd != null && controller.hoveredId() == null) {
                done = true;
                WardrobeUi.openWheelEditor(parent);
            } else {
                finish(controller.hovered() >= 0);
            }
        }
        return true;
    }

    @Override
    protected boolean pointerUp(double x, double y, int button) {
        return true;
    }

    @Override
    protected boolean wheel(double x, double y, double dx, double dy) {
        return true;
    }

    @Override
    public void onClose() {
        if (done) ClientCompat.openScreen(parent);
        else finish(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean held() {
        try {
            return keyHeld.getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void finish(boolean play) {
        if (done) return;
        done = true;
        Wardrobe wd = WardrobeController.wardrobe();
        String id = play ? controller.hoveredId() : null;
        ClientCompat.openScreen(parent);
        if (id != null && wd != null) wd.playEmote(id);
    }
}
