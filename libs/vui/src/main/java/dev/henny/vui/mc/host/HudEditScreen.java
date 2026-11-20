package dev.henny.vui.mc.host;

import dev.henny.vui.bind.Model;
import dev.henny.vui.event.Keys;
import dev.henny.vui.hud.HudEditor;
import dev.henny.vui.hud.HudWidget;
import dev.henny.vui.mc.VuiRuntime;
import dev.henny.vui.mc.compat.Mc;
import dev.henny.vui.mc.input.ModState;
import dev.henny.vui.mc.input.MouseButtons;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HudEditScreen extends InputScreen {
    private static final int KEY_R = 'R';
    private static final int KEY_H = 'H';

    private final HudHost hud;
    private final Screen parent;
    private final Model bar = new Model().value("selected", "");
    private DocumentView barView;

    HudEditScreen(HudHost hud, Screen parent) {
        super(Component.literal("vui:hud-editor"));
        this.hud = hud;
        this.parent = parent;
    }

    public HudEditor editor() {
        return hud.editor();
    }

    @Override
    protected void init() {
        editor().active(true);
        if (barView == null) barView = new DocumentView(VuiRuntime.get().vui().open("vui:hud/editor", bar));
    }

    @Override
    public void removed() {
        editor().active(false);
        hud.save();
        if (barView == null) return;
        VuiRuntime.get().vui().close(barView.document());
        barView = null;
    }

    @Override
    public void onClose() {
        Mc.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void draw(Object graphics, int mouseX, int mouseY, float delta) {
        hud.draw(graphics);
        HudWidget selected = editor().selected();
        bar.value("selected", selected == null ? "" : selected.id());
        if (barView != null) barView.draw(graphics, width, height, false);
    }

    @Override
    protected boolean pointerMove(double x, double y) {
        editor().snapping(!ModState.alt());
        return editor().pointerMove((float) x, (float) y);
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        return button == MouseButtons.LEFT && editor().pointerDown((float) x, (float) y);
    }

    @Override
    protected boolean pointerUp(double x, double y, int button) {
        return button == MouseButtons.LEFT && editor().pointerUp((float) x, (float) y);
    }

    @Override
    protected boolean wheel(double x, double y, double dx, double dy) {
        return editor().scroll((float) x, (float) y, (float) dy);
    }

    @Override
    protected boolean keyDown(int key, int scancode) {
        float step = ModState.shift() ? 10f : 1f;
        HudWidget selected = editor().selected();
        return switch (key) {
            case Keys.LEFT -> editor().nudge(-step, 0);
            case Keys.RIGHT -> editor().nudge(step, 0);
            case Keys.UP -> editor().nudge(0, -step);
            case Keys.DOWN -> editor().nudge(0, step);
            case KEY_R -> act(selected, () -> editor().reset(selected));
            case KEY_H -> act(selected, () -> editor().toggleVisible(selected));
            default -> false;
        };
    }

    private static boolean act(HudWidget selected, Runnable action) {
        if (selected == null) return false;
        action.run();
        return true;
    }
}
