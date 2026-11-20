package dev.henny.vibeyclient.module.servers.friends;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;
import dev.henny.vui.event.PointerEvent;

public final class FriendsController {
    public interface Actions {
        void join(FriendItem item);

        void add(FriendItem item);

        void decline(FriendItem item);

        void openFriends();

        void signIn();

        void hover(FriendItem item);

        void grab(FriendItem item, float x, float y);

        void dragTo(float x, float y);

        void dropAt(float x, float y);
    }

    public final State<String> state = new State<>("off");
    public final ListState<FriendItem> rows = new ListState<>();
    public final State<Float> left = new State<>(0f);
    public final State<Float> top = new State<>(0f);
    public final State<Float> width = new State<>(0f);
    public final State<Float> height = new State<>(0f);
    public final State<Boolean> cardOpen = new State<>(false);
    public final State<String> cardName = new State<>("");
    public final State<String> cardText = new State<>("");
    public final State<String> cardHead = new State<>("");
    public final State<Float> cardX = new State<>(0f);
    public final State<Float> cardY = new State<>(0f);
    public final State<Boolean> dragging = new State<>(false);
    public final State<String> dragLabel = new State<>("");
    public final State<Float> dragX = new State<>(0f);
    public final State<Float> dragY = new State<>(0f);
    private final Actions actions;

    FriendsController(Actions actions) {
        this.actions = actions;
    }

    public String tr(String key) {
        return Lang.tr(key);
    }

    public void join(FriendItem r) {
        actions.join(r);
    }

    public void add(FriendItem r) {
        actions.add(r);
    }

    public void decline(FriendItem r) {
        actions.decline(r);
    }

    public void openFriends() {
        actions.openFriends();
    }

    public void signIn() {
        actions.signIn();
    }

    public void hover(FriendItem r) {
        actions.hover(r);
    }

    public void unhover(FriendItem r) {
        actions.hover(null);
    }

    public void grab(FriendItem r, PointerEvent e) {
        actions.grab(r, e.x(), e.y());
    }

    public void dragMove(PointerEvent e) {
        actions.dragTo(e.x(), e.y());
    }

    public void dragEnd(PointerEvent e) {
        actions.dropAt(e.x(), e.y());
    }

    void place(float l, float t, float w, float h) {
        if (left.peek() != l) left.set(l);
        if (top.peek() != t) top.set(t);
        if (width.peek() != w) width.set(w);
        if (height.peek() != h) height.set(h);
    }

    void state(String s) {
        if (!s.equals(state.peek())) state.set(s);
    }
}
