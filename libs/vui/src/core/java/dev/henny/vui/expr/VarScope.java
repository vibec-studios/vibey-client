package dev.henny.vui.expr;

import dev.henny.vui.bind.Action;
import dev.henny.vui.bind.State;

public final class VarScope implements Scope {
    private final Scope parent;
    private final String[] names;
    private final State<?>[] values;

    public VarScope(Scope parent, String[] names, Object[] initial) {
        this.parent = parent;
        this.names = names;
        this.values = new State<?>[names.length];
        for (int i = 0; i < names.length; i++) values[i] = new State<>(i < initial.length ? initial[i] : null);
    }

    public static VarScope of(Scope parent, String name, Object value) {
        return new VarScope(parent, new String[]{name}, new Object[]{value});
    }

    @SuppressWarnings("unchecked")
    public void set(int index, Object value) {
        ((State<Object>) values[index]).set(value);
    }

    @SuppressWarnings("unchecked")
    public void set(String name, Object value) {
        int i = indexOf(name);
        if (i >= 0) ((State<Object>) values[i]).set(value);
    }

    public Object peek(int index) {
        return values[index].peek();
    }

    @Override
    public Object lookup(String name) {
        int i = indexOf(name);
        if (i >= 0) return Values.unwrap(values[i].get());
        return parent == null ? MISSING : parent.lookup(name);
    }

    @Override
    public boolean assign(String name, Object value) {
        int i = indexOf(name);
        if (i >= 0) {
            set(i, value);
            return true;
        }
        return parent != null && parent.assign(name, value);
    }

    @Override
    public Object call(String name, Object[] args) {
        int i = indexOf(name);
        if (i >= 0 && values[i].peek() instanceof Action a) return a.invoke(args);
        return parent == null ? MISSING : parent.call(name, args);
    }

    @Override
    public Scope parent() {
        return parent;
    }

    private int indexOf(String name) {
        for (int i = 0; i < names.length; i++) if (names[i].equals(name)) return i;
        return -1;
    }
}
