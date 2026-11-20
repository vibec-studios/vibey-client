package dev.henny.vibeyclient.module.screens.edit;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.screens.apply.Item;

public final class KeyInput {
    private KeyInput() {}

    public static void key(EditSession e, int nativeKey, boolean ctrl, boolean shift) {
        int key = Keys.fromNative(nativeKey);
        int letter = ClientCompat.layoutLetter(key);
        ctrl = ctrl || ClientCompat.controlDown();
        shift = shift || ClientCompat.shiftDown();
        if (key == Keys.ESCAPE) {
            if (e.menu != null) e.menu = null;
            else EditMode.leave();
            return;
        }
        if (ctrl && letter == Keys.Z) {
            if (shift) EditActions.redo(e);
            else EditActions.undo(e);
            return;
        }
        if (ctrl && letter == Keys.Y) {
            EditActions.redo(e);
            return;
        }
        Item item = e.selectedItem();
        if (item == null) return;
        int step = shift ? 10 : 1;
        if (key == Keys.DELETE || key == Keys.BACKSPACE || (!ctrl && letter == Keys.H)) EditActions.toggleHide(e, item);
        else if (!ctrl && letter == Keys.R) EditActions.reset(e, item);
        else if (key == Keys.LEFT) EditActions.nudge(e, item, -step, 0);
        else if (key == Keys.RIGHT) EditActions.nudge(e, item, step, 0);
        else if (key == Keys.UP) EditActions.nudge(e, item, 0, -step);
        else if (key == Keys.DOWN) EditActions.nudge(e, item, 0, step);
    }
}
