package dev.henny.vui.event;

import dev.henny.vui.dom.Element;

public final class KeyEvent extends Event {
    private int key;
    private int scancode;
    private int mods;
    private int codepoint;
    private boolean repeat;

    public KeyEvent key(String type, Element target, int key, int scancode, int mods, boolean repeat) {
        reset(type, target, true, null);
        this.key = key;
        this.scancode = scancode;
        this.mods = mods;
        this.codepoint = 0;
        this.repeat = repeat;
        return this;
    }

    public KeyEvent character(Element target, int codepoint, int mods) {
        reset("char", target, true, null);
        this.key = 0;
        this.scancode = 0;
        this.mods = mods;
        this.codepoint = codepoint;
        this.repeat = false;
        return this;
    }

    public int key() {
        return key;
    }

    public int scancode() {
        return scancode;
    }

    public int mods() {
        return mods;
    }

    public int codepoint() {
        return codepoint;
    }

    public boolean repeat() {
        return repeat;
    }

    public boolean shift() {
        return (mods & Mods.SHIFT) != 0;
    }

    public boolean ctrl() {
        return (mods & (Mods.CTRL | Mods.SUPER)) != 0;
    }

    public boolean alt() {
        return (mods & Mods.ALT) != 0;
    }
}
