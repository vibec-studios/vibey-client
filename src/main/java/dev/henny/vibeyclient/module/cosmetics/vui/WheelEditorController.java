package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

public final class WheelEditorController {
    static final float BAR = 25;

    public final ListState<WheelSlice> slices = new ListState<>();
    public final ListState<EmoteRow> emotes = new ListState<>();
    public final State<Integer> selected = new State<>(0);
    public final State<WheelFrame> frame = new State<>(null);
    public final State<Float> paneWidth = new State<>(160f);
    public final State<String> slotLabel = new State<>("");
    public final State<String> slotName = new State<>("");
    public final State<String> count = new State<>("");
    public final State<Boolean> available = new State<>(true);
    private final Runnable close;
    private WheelGeometry geo;
    private String signature = "";

    WheelEditorController(Runnable close) {
        this.close = close;
    }

    public void layout(float width, float height) {
        float pane = Math.max(150, Math.min(300, width * 2 / 5f));
        float paneH = height - BAR;
        paneWidth.set(pane);
        geo = WheelGeometry.fit(pane / 2f, paneH / 2f - 6, Math.min(pane - 12, paneH - 30));
        frame.set(WheelFrame.of(geo, Math.round(geo.bottom() + 6)));
        signature = "";
        sync();
    }

    public void sync() {
        Wardrobe wd = WardrobeController.wardrobe();
        available.set(wd != null);
        String sig = WheelSlices.signature(wd) + selected.peek() + Lang.code();
        if (geo != null && !sig.equals(signature)) {
            signature = sig;
            slices.replaceAll(WheelSlices.of(geo, wd));
            emotes.replaceAll(EmoteRows.of(wd, selected.peek()));
        }
        labels(wd);
    }

    public void pick(WheelSlice s) {
        selected.set(s.index());
        sync();
    }

    public void clear(WheelSlice s) {
        Wardrobe wd = WardrobeController.wardrobe();
        if (wd == null) return;
        wd.setWheel(s.index(), null);
        selected.set(s.index());
        sync();
    }

    public void assign(EmoteRow e) {
        Wardrobe wd = WardrobeController.wardrobe();
        if (wd == null || e.locked()) return;
        int sel = selected.peek();
        String previous = wd.wheel(sel);
        int other = EmoteRows.slotOf(wd, e.id());
        if (other >= 0 && other != sel) wd.setWheel(other, previous);
        wd.setWheel(sel, e.id());
        selected.set(nextEmpty(wd, sel));
        sync();
    }

    public void clearAll() {
        Wardrobe wd = WardrobeController.wardrobe();
        if (wd == null) return;
        for (int i = 0; i < Wardrobe.WHEEL_SLOTS; i++) wd.setWheel(i, null);
        selected.set(0);
        sync();
    }

    public void done() {
        close.run();
    }

    private void labels(Wardrobe wd) {
        int n = 0;
        if (wd != null) for (int i = 0; i < Wardrobe.WHEEL_SLOTS; i++) if (wd.wheel(i) != null) n++;
        count.set(Lang.tr("ui.wheel.count", n));
        int sel = selected.peek();
        WheelSlice s = sel < slices.get().size() ? slices.get().get(sel) : null;
        slotLabel.set(Lang.tr("ui.wheel.slot", sel + 1));
        slotName.set(s == null || s.empty() ? Lang.tr("ui.wheel.empty") : s.name());
    }

    private static int nextEmpty(Wardrobe wd, int after) {
        for (int k = 1; k <= Wardrobe.WHEEL_SLOTS; k++) {
            int i = Math.floorMod(after + k, Wardrobe.WHEEL_SLOTS);
            if (wd.wheel(i) == null) return i;
        }
        return after;
    }
}
