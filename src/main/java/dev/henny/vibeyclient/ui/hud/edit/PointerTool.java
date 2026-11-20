package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

public final class PointerTool {
    public enum Mode { IDLE, PRESS, MOVE, RESIZE, MARQUEE }

    private final EditSession s;
    private final HudLayout.Box tmp = new HudLayout.Box();
    private Mode mode = Mode.IDLE;
    private double pressX;
    private double pressY;
    private HudElement pressed;
    private boolean toggle;
    private boolean wasSelected;
    private MoveGesture move;
    private ResizeGesture resize;
    private MarqueeGesture marquee;
    private HudElement hovered;
    private int hoverHandle = -1;
    private Runnable onDrag = () -> {};

    public PointerTool(EditSession s) {
        this.s = s;
    }

    public void onDrag(Runnable r) {
        onDrag = r;
    }

    public Mode mode() {
        return mode;
    }

    public boolean busy() {
        return mode == Mode.MOVE || mode == Mode.RESIZE;
    }

    public HudElement hovered() {
        return hovered;
    }

    public int handle() {
        return mode == Mode.RESIZE ? resize.handle() : hoverHandle;
    }

    public MarqueeGesture marquee() {
        return mode == Mode.MARQUEE ? marquee : null;
    }

    public void hover(double mx, double my, boolean blocked) {
        hovered = null;
        hoverHandle = -1;
        if (mode == Mode.MOVE || mode == Mode.RESIZE) {
            hovered = s.selection.primary();
            return;
        }
        if (blocked || mode != Mode.IDLE) return;
        HudElement p = s.selection.primary();
        if (s.selection.size() == 1 && p != null) hoverHandle = Handles.at(s.box(p, tmp), mx, my);
        hovered = hoverHandle >= 0 ? p : s.elementAt(mx, my);
    }

    public boolean down(double mx, double my, int button, boolean add) {
        pressX = mx;
        pressY = my;
        HudElement p = s.selection.primary();
        int h = button == 0 && s.selection.size() == 1 && p != null ? Handles.at(s.box(p, tmp), mx, my) : -1;
        if (h >= 0) {
            resize = new ResizeGesture(s, p, h, mx, my);
            mode = Mode.RESIZE;
            return true;
        }
        HudElement hit = s.elementAt(mx, my);
        if (hit == null) {
            if (button != 0) return false;
            marquee = new MarqueeGesture(s, mx, my, add);
            mode = Mode.MARQUEE;
            return true;
        }
        pressed = hit;
        wasSelected = s.selection.contains(hit);
        toggle = add && button == 0;
        if (toggle) s.selection.add(hit);
        else if (!wasSelected) s.selection.only(hit);
        mode = Mode.PRESS;
        return true;
    }

    public boolean drag(double mx, double my, boolean free, boolean shape) {
        switch (mode) {
            case PRESS -> {
                if (Math.abs(mx - pressX) + Math.abs(my - pressY) < 2) return true;
                move = new MoveGesture(s, pressX, pressY);
                mode = Mode.MOVE;
                onDrag.run();
                move.drag(mx, my, free);
            }
            case MOVE -> move.drag(mx, my, free);
            case RESIZE -> {
                onDrag.run();
                resize.drag(mx, my, free, shape);
            }
            case MARQUEE -> marquee.drag(mx, my);
            default -> {
                return false;
            }
        }
        return true;
    }

    public boolean up() {
        Mode was = mode;
        switch (mode) {
            case PRESS -> {
                if (toggle && wasSelected) s.selection.remove(pressed);
                else if (!toggle && s.selection.size() > 1) s.selection.only(pressed);
            }
            case MOVE -> move.end();
            case RESIZE -> resize.end();
            default -> {
            }
        }
        mode = Mode.IDLE;
        pressed = null;
        move = null;
        resize = null;
        marquee = null;
        return was != Mode.IDLE;
    }
}
