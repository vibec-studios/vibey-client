package dev.henny.vui.event;

import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.render.Cursor;
import dev.henny.vui.style.Prop;

public final class EventSystem {
    private final Document document;
    private final Dispatcher dispatcher = new Dispatcher();
    private final PointerEvent pointer = new PointerEvent();
    private final KeyEvent keyEvent = new KeyEvent();
    private final FocusManager focus;
    private final TooltipController tooltips;
    private final PointerState state = new PointerState();
    private final Keyboard keyboard;
    private Cursor cursor = Cursor.DEFAULT;

    public EventSystem(Document document) {
        this.document = document;
        this.focus = new FocusManager(document);
        this.tooltips = new TooltipController(document);
        this.keyboard = new Keyboard(document, this);
    }

    public FocusManager focus() {
        return focus;
    }

    public TooltipController tooltips() {
        return tooltips;
    }

    public PointerState pointerState() {
        return state;
    }

    public Element hovered() {
        return state.hovered;
    }

    public Element hit(float x, float y) {
        return HitTester.hit(document.root(), document.overlays().open(), x, y);
    }

    public boolean pointerMove(float x, float y, int mods) {
        float dx = x - state.lastX;
        float dy = y - state.lastY;
        state.lastX = x;
        state.lastY = y;
        Element target = hit(x, y);
        Hover.update(state, target, this);
        if (state.pressed != null) {
            Drag.move(state, this, x, y, dx, dy, mods);
            send(pointer.pointer(EventNames.POINTER_MOVE, state.pressed, x, y, state.button, mods).delta(dx, dy), true);
        } else if (target != null) {
            send(pointer.pointer(EventNames.POINTER_MOVE, target, x, y, -1, mods).delta(dx, dy), true);
        }
        updateCursor(state.pressed != null ? state.pressed : target);
        return target != null;
    }

    public boolean pointerDown(float x, float y, int button, int mods) {
        state.lastX = x;
        state.lastY = y;
        tooltips.hide();
        Element target = hit(x, y);
        Hover.update(state, target, this);
        state.press(target, button, x, y);
        Hover.active(target, true);
        Element focusTarget = target == null ? null : FocusManager.focusableAncestor(target);
        focus.focus(focusTarget, false);
        if (target == null) return false;
        send(pointer.pointer(EventNames.POINTER_DOWN, target, x, y, button, mods), true);
        return true;
    }

    public boolean pointerUp(float x, float y, int button, int mods) {
        Element target = hit(x, y);
        Element pressed = state.pressed;
        Element receiver = pressed != null ? pressed : target;
        if (receiver != null) send(pointer.pointer(EventNames.POINTER_UP, receiver, x, y, button, mods), true);
        if (state.dragging && pressed != null) {
            pressed.state(dev.henny.vui.style.StateFlags.DRAGGING, false);
            send(pointer.pointer(EventNames.DRAG_END, pressed, x, y, button, mods).delta(x - state.pressX, y - state.pressY), true);
        } else if (pressed != null && target != null) {
            Clicks.release(state, this, pressed, target, x, y, button, mods, document.time());
        }
        Hover.active(pressed, false);
        state.release();
        Hover.update(state, target, this);
        return receiver != null;
    }

    public boolean scroll(float x, float y, float dx, float dy, int mods) {
        tooltips.hide();
        Element target = hit(x, y);
        if (target == null) return false;
        PointerEvent e = pointer.pointer(EventNames.WHEEL, target, x, y, -1, mods).delta(dx, dy);
        send(e, true);
        if (!e.prevented()) WheelScroll.apply(target, dx, dy);
        return true;
    }

    public boolean keyDown(int key, int scancode, int mods, boolean repeat) {
        tooltips.hide();
        return keyboard.down(keyEvent, key, scancode, mods, repeat);
    }

    public boolean keyUp(int key, int scancode, int mods) {
        Element target = focus.focused() != null ? focus.focused() : document.root();
        return send(keyEvent.key(EventNames.KEY_UP, target, key, scancode, mods, false), true);
    }

    public boolean charTyped(int codepoint, int mods) {
        Element target = focus.focused();
        if (target == null) return false;
        KeyEvent e = keyEvent.character(target, codepoint, mods);
        send(e, true);
        return true;
    }

    public void click(Element target) {
        float x = target.box().x + target.box().width / 2f;
        float y = target.box().y + target.box().height / 2f;
        send(pointer.pointer(EventNames.CLICK, target, x, y, PointerEvent.LEFT, 0).clicks(1), true);
    }

    public boolean emit(Element target, String type, Object detail) {
        Event e = new Event().reset(type, target, false, detail);
        return dispatcher.emit(target, e);
    }

    public boolean dispatch(Element target, String type, Object detail) {
        Event e = new Event().reset(type, target, EventNames.bubbles(type), detail);
        return dispatcher.dispatch(e, false);
    }

    public void signal(Element target, String type) {
        Event e = new Event().reset(type, target, false, null);
        dispatcher.emit(target, e);
        target.behavior().handle(target, e);
    }

    public boolean send(Event e, boolean input) {
        return dispatcher.dispatch(e, input);
    }

    public void forget(Element e) {
        focus.forget(e);
        tooltips.forget(e);
        state.forget(e);
    }

    private void updateCursor(Element target) {
        Cursor next = target == null ? Cursor.DEFAULT : Cursor.fromCss(target.style().keyword(Prop.CURSOR));
        if (next == cursor) return;
        cursor = next;
        document.vui().backend().input().setCursor(next);
    }
}
