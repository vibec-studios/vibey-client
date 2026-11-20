package dev.henny.vibeyclient.module.visuals.items.edit;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.visuals.items.core.EditSession;
import dev.henny.vibeyclient.module.visuals.items.core.HandPick;
import dev.henny.vibeyclient.ui.vui.SyncedScreen;
import dev.henny.vui.mc.host.DocumentView;
import dev.henny.vui.mc.input.ModState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

public final class ItemEditScreen extends SyncedScreen {
    public static final String DOC = "vibeyclient:items/editor";
    public static boolean drawPointer;
    private static final int ENTER = 257, KP_ENTER = 335, TAB = 258, SPACE = 32, R = 82;

    private final ItemEditController c;
    private final EditGestures gestures;
    private final Screen parent;
    private boolean docPressed;
    private boolean done;
    private double mouseX = -1, mouseY = -1;

    ItemEditScreen(Screen parent, EditSession session) {
        this(parent, new ItemEditController(session));
    }

    private ItemEditScreen(Screen parent, ItemEditController c) {
        super(DOC, c, parent);
        this.parent = parent;
        this.c = c;
        this.gestures = new EditGestures(c);
        c.onFinish(this::finish);
        background(false);
    }

    public ItemEditController controller() {
        return c;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void sync() {
    }

    @Override
    protected void draw(Object graphics, int mx, int my, float delta) {
        if (!gestures.dragging()) {
            mouseX = mx;
            mouseY = my;
        }
        c.frame(side(), width, height, hovered(), emptyHand());
        c.pointer(drawPointer, mx, my);
        super.draw(graphics, mx, my, delta);
    }

    private boolean hovered() {
        return mouseX >= 0 && HandPick.over(c.session.current(), side(), mouseX, mouseY, width, height);
    }

    private boolean emptyHand() {
        var p = Minecraft.getInstance().player;
        if (p == null) return true;
        return (c.session.hand() == EditSession.OFF ? p.getOffhandItem() : p.getMainHandItem()).isEmpty();
    }

    int side() {
        return c.session.hand() == EditSession.OFF ? -mainSide() : mainSide();
    }

    static int mainSide() {
        var p = Minecraft.getInstance().player;
        return p == null || p.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
    }

    private boolean overChrome(double x, double y) {
        DocumentView v = view();
        return c.menuOpen() || !v.document().overlays().open().isEmpty() || v.hits(x, y);
    }

    @Override
    protected boolean pointerMove(double x, double y) {
        mouseX = x;
        mouseY = y;
        view().move(x, y);
        gestures.drag(x, y, side(), height);
        return true;
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        if (overChrome(x, y)) {
            docPressed = true;
            return view().down(x, y, button);
        }
        if (button == 1) {
            c.pickHand(HandPick.nearest(c.session, mainSide(), x, y, width, height) == EditSession.OFF ? "off" : "main");
            c.openMenu((float) x, (float) y);
            return true;
        }
        gestures.grab(x, y, mainSide(), width, height, ModState.control(), ModState.alt());
        return true;
    }

    @Override
    protected boolean pointerUp(double x, double y, int button) {
        if (docPressed) view().up(x, y, button);
        docPressed = false;
        gestures.release();
        return true;
    }

    @Override
    protected boolean wheel(double x, double y, double dx, double dy) {
        if (overChrome(x, y)) return view().scroll(x, y, dx, dy);
        double steps = dy != 0 ? dy : dx;
        gestures.wheel(x, y, mainSide(), width, height, steps, ModState.shift());
        return true;
    }

    @Override
    protected boolean keyDown(int key, int scancode) {
        if (!c.menuOpen()) {
            if (key == ENTER || key == KP_ENTER) {
                finish(true);
                return true;
            }
            if (key == TAB) {
                c.pickHand(c.session.hand() == EditSession.OFF ? "main" : "off");
                return true;
            }
            if (key == R) {
                c.session.reset();
                return true;
            }
            if (key == SPACE) {
                var p = Minecraft.getInstance().player;
                if (p != null) p.swing(c.session.hand() == EditSession.OFF ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                return true;
            }
        }
        return super.keyDown(key, scancode);
    }

    @Override
    public void onClose() {
        finish(false);
    }

    @Override
    public void removed() {
        super.removed();
        if (!done) {
            done = true;
            ItemEditor.finish(c.session, false);
        }
    }

    void finish(boolean save) {
        if (done) return;
        done = true;
        ItemEditor.finish(c.session, save);
        ClientCompat.openScreen(parent);
    }
}
