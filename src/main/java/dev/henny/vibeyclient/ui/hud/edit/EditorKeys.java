package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vui.event.Keys;

public final class EditorKeys {
    private EditorKeys() {}

    public static boolean handle(EditSession s, EditActions a, int key, int letter, boolean ctrl, boolean shift) {
        if (ctrl && shortcut(s, a, letter, shift)) return true;
        if (s.selection.isEmpty()) return false;
        int step = shift ? 10 : 1;
        switch (key) {
            case Keys.LEFT -> a.nudge(-step, 0);
            case Keys.RIGHT -> a.nudge(step, 0);
            case Keys.UP -> a.nudge(0, -step);
            case Keys.DOWN -> a.nudge(0, step);
            case Keys.DELETE, Keys.BACKSPACE -> a.hideSelection();
            default -> {
                if (ctrl) return false;
                if (letter == 'H') a.hideSelection();
                else if (letter == 'R') a.resetSelection();
                else return false;
            }
        }
        return true;
    }

    private static boolean shortcut(EditSession s, EditActions a, int letter, boolean shift) {
        switch (letter) {
            case 'Z' -> {
                if (shift) a.redo();
                else a.undo();
            }
            case 'Y' -> a.redo();
            case 'A' -> a.selectAll();
            case 'D' -> a.duplicateSelection();
            default -> {
                return false;
            }
        }
        return true;
    }
}
