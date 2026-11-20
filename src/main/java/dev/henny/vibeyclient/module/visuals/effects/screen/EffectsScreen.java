package dev.henny.vibeyclient.module.visuals.effects.screen;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPart;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPreview;
import dev.henny.vibeyclient.ui.vui.SyncedScreen;
import dev.henny.vibeyclient.ui.vui.VibeyVui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;

public final class EffectsScreen extends SyncedScreen {
    public static final String DOC = "vibeyclient:effects/editor";
    private static final float TURN = 0.45f;

    private final EffectsController controller;
    private boolean looking;
    private double lastX, lastY;

    private EffectsScreen(Screen parent, EffectsController controller) {
        super(DOC, controller, parent);
        this.controller = controller;
        background(!ClientCompat.inWorld());
        pause(false);
    }

    public static EffectsScreen open(Screen parent) {
        EffectsPart part = EffectsPart.get();
        if (part == null) return null;
        VibeyVui.init();
        EffectsScreen screen = new EffectsScreen(parent, new EffectsController(part, () -> ClientCompat.openScreen(parent)));
        ClientCompat.openScreen(screen);
        return screen;
    }

    public static void openFromMenu() {
        open(ClientCompat.screen());
    }

    public EffectsController controller() {
        return controller;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void sync() {
        controller.sync();
    }

    @Override
    public void removed() {
        super.removed();
        EffectsPreview.clear();
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        if (!view().hits(x, y) && ClientCompat.inWorld()) {
            looking = true;
            lastX = x;
            lastY = y;
        }
        return super.pointerDown(x, y, button);
    }

    @Override
    protected boolean pointerMove(double x, double y) {
        if (looking) turn(x - lastX, y - lastY);
        lastX = x;
        lastY = y;
        return super.pointerMove(x, y);
    }

    @Override
    protected boolean pointerUp(double x, double y, int button) {
        looking = false;
        return super.pointerUp(x, y, button);
    }

    private static void turn(double dx, double dy) {
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        float yaw = p.getYRot() + (float) dx * TURN;
        float pitch = Math.max(-90f, Math.min(90f, p.getXRot() + (float) dy * TURN));
        p.setYRot(yaw);
        p.setXRot(pitch);
        p.yRotO = yaw;
        p.xRotO = pitch;
        p.setYHeadRot(yaw);
    }
}
