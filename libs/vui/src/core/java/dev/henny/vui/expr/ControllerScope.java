package dev.henny.vui.expr;

import dev.henny.vui.access.Members;

public final class ControllerScope implements Scope {
    private final Object controller;
    private final Functions functions;

    public ControllerScope(Object controller, Functions functions) {
        this.controller = controller;
        this.functions = functions == null ? new Functions() : functions;
    }

    @Override
    public Object lookup(String name) {
        if (controller == null) return MISSING;
        if (name.equals("this")) return controller;
        return Members.get(controller, name);
    }

    @Override
    public boolean assign(String name, Object value) {
        return Members.set(controller, name, value);
    }

    @Override
    public Object call(String name, Object[] args) {
        Object result = controller == null ? MISSING : Members.call(controller, name, args);
        if (result != MISSING) return result;
        return functions.call(name, args);
    }

    @Override
    public Scope parent() {
        return null;
    }

    @Override
    public Object controller() {
        return controller;
    }

    public Functions functions() {
        return functions;
    }
}
