package dev.henny.vui.event;

import java.util.List;

public final class Handler {
    private final String type;
    private final List<String> modifiers;
    private final Listener listener;
    private final boolean capture;
    private final boolean once;
    private boolean spent;

    public Handler(String type, List<String> modifiers, Listener listener) {
        this.type = EventNames.canonical(type);
        this.modifiers = modifiers;
        this.listener = listener;
        this.capture = modifiers.contains("capture");
        this.once = modifiers.contains("once");
    }

    public String type() {
        return type;
    }

    public boolean capture() {
        return capture;
    }

    public boolean accepts(Event e) {
        if (spent) return false;
        return Modifiers.accept(modifiers, e);
    }

    public void fire(Event e) {
        if (modifiers.contains("self") && e.target() != e.currentTarget()) return;
        if (once) spent = true;
        if (modifiers.contains("prevent")) e.preventDefault();
        listener.handle(e);
        if (modifiers.contains("stop")) e.stopPropagation();
    }
}
