package dev.henny.vibeyclient.module.visuals.items.edit;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.visuals.items.core.EditSession;
import dev.henny.vibeyclient.module.visuals.items.core.HandPose;
import dev.henny.vibeyclient.module.visuals.items.core.HandPreset;
import dev.henny.vibeyclient.module.visuals.items.core.HandProjection;
import dev.henny.vui.bind.State;

import java.util.List;
import java.util.function.Consumer;

public final class ItemEditController {
    private static final float MIN_RING = 18;

    public final State<String> hand = new State<>("main");
    public final State<Boolean> ringOn = new State<>(false);
    public final State<Float> ringLeft = new State<>(0f);
    public final State<Float> ringTop = new State<>(0f);
    public final State<Float> ringSize = new State<>(0f);
    public final State<String> tag = new State<>("");
    public final State<Float> tagLeft = new State<>(0f);
    public final State<Float> tagTop = new State<>(0f);
    public final State<Boolean> empty = new State<>(false);
    public final State<Boolean> grabbing = new State<>(false);
    public final State<String> panel = new State<>("");
    public final State<Boolean> pointerOn = new State<>(false);
    public final State<Float> pointerX = new State<>(0f);
    public final State<Float> pointerY = new State<>(0f);
    public final State<EditMenu.Menu> menu = new State<>(new EditMenu.Menu(0, 0, List.of()));

    final EditSession session;
    private Consumer<Boolean> finish = save -> {};

    ItemEditController(EditSession session) {
        this.session = session;
    }

    void onFinish(Consumer<Boolean> finish) {
        this.finish = finish;
    }

    public void pickHand(String h) {
        session.select("off".equals(h) ? EditSession.OFF : EditSession.MAIN);
        hand.set(session.hand() == EditSession.OFF ? "off" : "main");
    }

    public void save() {
        finish.accept(true);
    }

    public void cancel() {
        finish.accept(false);
    }

    public void menuPick(EditMenu.Entry e) {
        EditMenu.pick(session, e.id());
        panel.set("");
    }

    void openMenu(float x, float y) {
        menu.set(EditMenu.build(session, x, y));
        panel.set("menu");
    }

    boolean menuOpen() {
        return "menu".equals(panel.peek());
    }

    void frame(int side, float width, float height, boolean hovered, boolean emptyHand) {
        HandPose p = session.current();
        float[] at = HandProjection.toScreen(HandProjection.anchor(p, side), width, height);
        float size = Math.max(MIN_RING, 2 * HandProjection.radius(p, side, height));
        update(ringOn, hovered || grabbing.peek());
        update(ringSize, (float) Math.round(size));
        update(ringLeft, (float) Math.round(at[0] - size / 2));
        update(ringTop, (float) Math.round(at[1] - size / 2));
        update(tagLeft, (float) Math.round(at[0] - size / 2));
        update(tagTop, (float) Math.round(at[1] - size / 2 - 12));
        update(tag, label(p));
        update(empty, emptyHand);
    }

    void pointer(boolean on, double x, double y) {
        update(pointerOn, on);
        update(pointerX, (float) Math.round(x - 2));
        update(pointerY, (float) Math.round(y - 2));
    }

    private static String label(HandPose p) {
        HandPreset preset = HandPreset.match(p);
        String name = preset == null ? Lang.tr("visuals.items.preset.custom") : Lang.tr("visuals.items.preset." + preset.id());
        return name + " · " + Math.round(p.scale() * 100) + " %";
    }

    private static <T> void update(State<T> s, T v) {
        if (!v.equals(s.peek())) s.set(v);
    }
}
