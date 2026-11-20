package dev.henny.r3.interact;

import dev.henny.r3.anchor.WorldQuery;
import dev.henny.r3.hint.Hint;
import dev.henny.r3.hint.HintBook;
import dev.henny.r3.hint.HintStore;
import dev.henny.r3.hint.HintText;
import dev.henny.r3.input.Gesture;
import dev.henny.r3.input.GestureBindings;
import dev.henny.r3.input.HeldKeys;
import dev.henny.r3.input.InputCode;
import dev.henny.r3.panel.PointerButton;
import dev.henny.r3.panel.PointerContent;

import java.util.ArrayList;
import java.util.List;

public final class Interaction {
    private final InteractionConfig config;
    private final GestureBindings bindings;
    private final HeldKeys held;
    private final HintBook hints;
    private final UndoHistory undo;
    private final HoverFinder finder = new HoverFinder();
    private final HoverState hover = new HoverState();
    private final HoverTracker tracker = new HoverTracker();
    private final GrabSession grab;
    private final EdgeDrag drag = new EdgeDrag();
    private final Pointer pointer = new Pointer();
    private final WheelGestures wheel;
    private final PressGestures presses;
    private final List<InteractionListener> listeners = new ArrayList<>();
    private WorldQuery query;
    private InteractionState state = InteractionState.IDLE;
    private Interactable contentTarget;
    private float time;

    public Interaction(InteractionConfig config, GestureBindings bindings, HeldKeys held, HintStore hintStore) {
        this.config = config;
        this.bindings = bindings;
        this.held = held;
        this.hints = new HintBook(hintStore, config.hintSeconds);
        this.undo = new UndoHistory(config.undoDepth, config.wheelCoalesceSeconds);
        this.grab = new GrabSession(config);
        this.wheel = new WheelGestures(this);
        this.presses = new PressGestures(this);
    }

    public Interaction query(WorldQuery query) {
        this.query = query;
        return this;
    }

    public Interaction listen(InteractionListener listener) {
        listeners.add(listener);
        return this;
    }

    public void update(Pointer current, Iterable<? extends Interactable> candidates, float dt) {
        time += dt;
        pointer.set(current);
        switch (state) {
            case HOLDING -> grab.follow(pointer, query);
            case RESIZING -> drag.follow(pointer.ray());
            default -> finder.find(pointer, candidates, config, hover);
        }
        if (state == InteractionState.CONTENT_DRAG) tracker.dragMove(contentTarget, pointer.ray());
        else tracker.update(hover, state.capturing());
        hints.update(situation(), dt);
    }

    public boolean press(InputCode code) {
        return presses.handle(code);
    }

    boolean use(Interactable t) {
        PointerContent content = t.pointerContent();
        if (hover.zone() == HoverZone.CONTROL) emit(InteractionEventKind.ACTION, t, hover.controlId());
        if (content != null && hover.zone().onContent()) {
            boolean wants = content.pointerDown(hover.pixelX(), hover.pixelY(), PointerButton.PRIMARY);
            if (wants || hover.zone() == HoverZone.CONTROL) {
                tracker.pressedAt(hover.pixelX(), hover.pixelY());
                contentTarget = t;
                state = InteractionState.CONTENT_DRAG;
            }
        }
        return true;
    }

    boolean grabOrResize(Interactable t) {
        if (t.locked()) return false;
        if (hover.zone().onBorder() && t.rules().edgeResizable()) {
            drag.begin(t, hover.handle(), hover.hit());
            state = InteractionState.RESIZING;
            hints.performed(Hint.EDGE);
            emit(InteractionEventKind.RESIZE_STARTED, t, null);
            return true;
        }
        if (!t.rules().movable()) return false;
        grab.begin(t, hover.hit().distance(), hover.hit().localX(), hover.hit().localY(), false);
        startHolding(t);
        hints.performed(Hint.HOVER);
        return true;
    }

    private void startHolding(Interactable t) {
        tracker.exit();
        state = InteractionState.HOLDING;
        emit(InteractionEventKind.GRAB_STARTED, t, null);
    }

    public boolean release(InputCode code) {
        if (state == InteractionState.HOLDING && !grab.clickToPlace() && bindings.released(Gesture.GRAB, code)) {
            place();
            return true;
        }
        if (state == InteractionState.RESIZING && bindings.released(Gesture.GRAB, code)) {
            commitDrag();
            return true;
        }
        if (state == InteractionState.CONTENT_DRAG && bindings.released(Gesture.USE, code)) {
            endContentDrag();
            return true;
        }
        return state.capturing() && code.device() == InputCode.Device.MOUSE;
    }

    public boolean wheel(double notches) {
        return wheel.handle(notches);
    }

    public void beginPlacement(Interactable target) {
        cancel();
        grab.beginNew(target);
        startHolding(target);
    }

    public boolean beginMove(Interactable target) {
        if (target.locked() || !target.rules().movable()) return false;
        cancel();
        double distance = pointer.ray().origin().distance(target.transform().position());
        grab.begin(target, distance, 0, 0, true);
        startHolding(target);
        return true;
    }

    public void cancel() {
        switch (state) {
            case HOLDING -> {
                Interactable t = grab.target();
                boolean fresh = grab.isNew();
                if (!fresh) grab.before().restore(t);
                grab.end();
                state = InteractionState.IDLE;
                emit(fresh ? InteractionEventKind.PLACEMENT_CANCELLED : InteractionEventKind.MOVE_CANCELLED, t, null);
            }
            case RESIZING -> {
                Interactable t = drag.target();
                drag.before().restore(t);
                drag.end();
                state = InteractionState.IDLE;
                emit(InteractionEventKind.RESIZE_CANCELLED, t, null);
            }
            case CONTENT_DRAG -> endContentDrag();
            default -> {
            }
        }
    }

    void place() {
        Interactable t = grab.target();
        boolean fresh = grab.isNew();
        if (!fresh && grab.before().differsFrom(t)) undo.push(t, grab.before(), UndoKind.MOVE, time);
        grab.end();
        state = InteractionState.IDLE;
        hints.performed(fresh ? Hint.PLACE : Hint.HOLD);
        emit(fresh ? InteractionEventKind.PLACED : InteractionEventKind.MOVED, t, null);
    }

    private void commitDrag() {
        Interactable t = drag.target();
        if (drag.before().differsFrom(t)) undo.push(t, drag.before(), UndoKind.RESIZE, time);
        drag.end();
        state = InteractionState.IDLE;
        emit(InteractionEventKind.RESIZED, t, null);
    }

    private void endContentDrag() {
        PointerContent content = contentTarget == null ? null : contentTarget.pointerContent();
        if (content != null) content.pointerUp(tracker.lastX(), tracker.lastY(), PointerButton.PRIMARY);
        contentTarget = null;
        state = InteractionState.IDLE;
    }

    public boolean undo() {
        if (state.capturing()) return false;
        UndoEntry entry = undo.pop();
        if (entry == null) return false;
        entry.before().restore(entry.target());
        emit(InteractionEventKind.UNDONE, entry.target(), null);
        return true;
    }

    public void forget(Interactable target) {
        if (grab.target() == target || drag.target() == target || contentTarget == target) cancel();
        if (hover.target() == target) hover.clear();
        tracker.forget(target);
        undo.forget(target);
    }

    private Hint situation() {
        if (state == InteractionState.HOLDING) return grab.clickToPlace() ? Hint.PLACE : Hint.HOLD;
        if (state != InteractionState.IDLE) return null;
        Interactable t = hover.target();
        if (t == null || t.locked()) return null;
        if (hover.zone().onBorder()) return Hint.EDGE;
        return t.rules().movable() ? Hint.HOVER : null;
    }

    boolean pressed(Gesture gesture, InputCode code) {
        return bindings.pressed(gesture, code, held);
    }

    boolean held(Gesture gesture) {
        return bindings.held(gesture, held);
    }

    void emit(InteractionEventKind kind, Interactable target, String controlId) {
        InteractionEvent event = new InteractionEvent(kind, target, controlId);
        for (InteractionListener l : listeners) l.onEvent(event);
    }

    void pushUndo(Interactable target, PoseSnapshot before, UndoKind kind) {
        undo.push(target, before, kind, time);
    }

    void performed(Hint hint) {
        hints.performed(hint);
    }

    public InteractionConfig config() {
        return config;
    }

    public GestureBindings bindings() {
        return bindings;
    }

    public InteractionState state() {
        return state;
    }

    public boolean capturing() {
        return state.capturing();
    }

    public HoverState hover() {
        return hover;
    }

    public GrabSession grab() {
        return grab;
    }

    public EdgeDrag edgeDrag() {
        return drag;
    }

    public Interactable held() {
        return state == InteractionState.HOLDING ? grab.target() : state == InteractionState.RESIZING ? drag.target() : null;
    }

    public UndoHistory history() {
        return undo;
    }

    public Hint hint() {
        return hints.current();
    }

    public String hintText() {
        return HintText.format(hints.current(), bindings);
    }
}
