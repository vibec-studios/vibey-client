package dev.henny.vui.event;

import java.util.List;
import java.util.Set;

public final class Modifiers {
    private static final Set<String> BEHAVIOUR = Set.of("stop", "prevent", "self", "once", "capture", "passive", "exact");

    private Modifiers() {
    }

    public static boolean accept(List<String> modifiers, Event e) {
        if (modifiers.isEmpty()) return true;
        if (e instanceof KeyEvent k) return acceptKey(modifiers, k);
        if (e instanceof PointerEvent p) return acceptPointer(modifiers, p);
        return true;
    }

    private static boolean acceptKey(List<String> modifiers, KeyEvent k) {
        boolean wantShift = false;
        boolean wantCtrl = false;
        boolean wantAlt = false;
        for (String m : modifiers) {
            if (BEHAVIOUR.contains(m)) continue;
            switch (m) {
                case "shift" -> wantShift = true;
                case "ctrl", "meta" -> wantCtrl = true;
                case "alt" -> wantAlt = true;
                default -> {
                    int key = Keys.byName(m);
                    if (key != 0 && k.key() != key && !(key == Keys.ENTER && Keys.isEnter(k.key()))) return false;
                }
            }
        }
        if (wantShift && !k.shift() || wantCtrl && !k.ctrl() || wantAlt && !k.alt()) return false;
        if (modifiers.contains("exact")) return k.shift() == wantShift && k.ctrl() == wantCtrl && k.alt() == wantAlt;
        return true;
    }

    private static boolean acceptPointer(List<String> modifiers, PointerEvent p) {
        for (String m : modifiers) {
            switch (m) {
                case "left" -> {
                    if (p.button() != PointerEvent.LEFT) return false;
                }
                case "right" -> {
                    if (p.button() != PointerEvent.RIGHT) return false;
                }
                case "middle" -> {
                    if (p.button() != PointerEvent.MIDDLE) return false;
                }
                case "shift" -> {
                    if (!p.shift()) return false;
                }
                case "ctrl" -> {
                    if (!p.ctrl()) return false;
                }
                case "alt" -> {
                    if (!p.alt()) return false;
                }
                default -> {
                }
            }
        }
        return true;
    }
}
