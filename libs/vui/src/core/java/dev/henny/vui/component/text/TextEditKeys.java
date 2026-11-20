package dev.henny.vui.component.text;

import dev.henny.vui.event.KeyEvent;
import dev.henny.vui.event.Keys;
import dev.henny.vui.render.Input;

public final class TextEditKeys {
    public enum Result { NONE, MOVED, EDITED, SUBMIT }

    private TextEditKeys() {
    }

    public static Result apply(TextEditState s, KeyEvent k, Input input) {
        boolean word = k.ctrl();
        boolean shift = k.shift();
        switch (k.key()) {
            case Keys.LEFT -> s.left(word, shift);
            case Keys.RIGHT -> s.right(word, shift);
            case Keys.HOME -> s.moveTo(0, shift);
            case Keys.END -> s.moveTo(s.length(), shift);
            case Keys.BACKSPACE -> {
                return s.backspace(word) ? Result.EDITED : Result.MOVED;
            }
            case Keys.DELETE -> {
                return s.delete(word) ? Result.EDITED : Result.MOVED;
            }
            case Keys.ENTER, Keys.KP_ENTER -> {
                return Result.SUBMIT;
            }
            default -> {
                return shortcut(s, k, input);
            }
        }
        return Result.MOVED;
    }

    private static Result shortcut(TextEditState s, KeyEvent k, Input input) {
        if (!k.ctrl()) return Result.NONE;
        switch (k.key()) {
            case Keys.A -> {
                s.selectAll();
                return Result.MOVED;
            }
            case Keys.C -> {
                if (s.hasSelection() && input != null) input.setClipboard(s.selected());
                return Result.MOVED;
            }
            case Keys.X -> {
                if (!s.hasSelection()) return Result.MOVED;
                if (input != null) input.setClipboard(s.selected());
                s.deleteSelection();
                return Result.EDITED;
            }
            case Keys.V -> {
                String clip = input == null ? null : input.clipboard();
                if (clip == null || clip.isEmpty()) return Result.MOVED;
                s.insert(clip);
                return Result.EDITED;
            }
            default -> {
                return Result.NONE;
            }
        }
    }
}
