package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

public final class WheelController {
    static final float TOP = 6;
    static final float HUD_BOTTOM = 44;
    static final float HINT = 16;

    public final ListState<WheelSlice> slices = new ListState<>();
    public final State<Integer> hovered = new State<>(-1);
    public final State<WheelFrame> frame = new State<>(null);
    public final State<String> title = new State<>("");
    public final State<String> hint = new State<>("");
    public final State<Float> dotX = new State<>(0f);
    public final State<Float> dotY = new State<>(0f);
    public final State<Boolean> available = new State<>(true);
    private WheelGeometry geo;
    private boolean sticky;
    private String signature = "";

    public void layout(float width, float height) {
        float half = Math.min(height / 2 - TOP - HINT, height / 2 - HUD_BOTTOM);
        float diameter = Math.max(80, Math.min(width - 16, 2 * half));
        geo = WheelGeometry.fit(width / 2f, height / 2f, diameter);
        frame.set(WheelFrame.of(geo, Math.round(geo.top() - HINT)));
        signature = "";
        sync();
    }

    public void sync() {
        Wardrobe wd = WardrobeController.wardrobe();
        available.set(wd != null);
        String sig = WheelSlices.signature(wd);
        if (geo != null && !sig.equals(signature)) {
            signature = sig;
            slices.replaceAll(WheelSlices.of(geo, wd));
        }
        labels();
    }

    public void sticky(boolean on) {
        sticky = on;
        labels();
    }

    public void hover(double x, double y) {
        if (geo == null) return;
        int i = geo.direction(x, y);
        if (i == hovered.peek()) return;
        hovered.set(i);
        if (i >= 0) {
            dotX.set(geo.dotX(i));
            dotY.set(geo.dotY(i));
        }
        labels();
    }

    public void pick(WheelSlice s) {
    }

    public void clear(WheelSlice s) {
    }

    public void hoverSlot(int i) {
        if (geo == null) return;
        if (i < 0) hover(geo.cx(), geo.cy());
        else hover(geo.sliceX(i) + geo.tile() / 2, geo.sliceY(i) + geo.tile() / 2);
    }

    public int hovered() {
        return hovered.peek();
    }

    public String hoveredId() {
        Wardrobe wd = WardrobeController.wardrobe();
        int i = hovered.peek();
        return wd == null || i < 0 ? null : wd.wheel(i);
    }

    private void labels() {
        int i = hovered.peek();
        WheelSlice s = i < 0 || i >= slices.get().size() ? null : slices.get().get(i);
        if (!available.peek()) {
            title.set(Lang.tr("ui.wheel.title"));
            hint.set(Lang.tr("ui.wheel.off"));
        } else if (s == null) {
            title.set(Lang.tr("ui.wheel.title"));
            hint.set(Lang.tr("ui.wheel.cancel"));
        } else if (s.empty()) {
            title.set(Lang.tr("ui.wheel.empty"));
            hint.set(Lang.tr("ui.wheel.fill"));
        } else {
            title.set(s.name());
            hint.set(Lang.tr(sticky ? "ui.wheel.click" : "ui.wheel.release"));
        }
    }
}
