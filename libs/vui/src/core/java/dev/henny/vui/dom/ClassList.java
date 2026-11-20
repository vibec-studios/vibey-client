package dev.henny.vui.dom;

import java.util.ArrayList;
import java.util.List;

public final class ClassList {
    private final List<String> fixed = new ArrayList<>();
    private final List<String> bound = new ArrayList<>();

    public boolean has(String name) {
        for (int i = 0; i < fixed.size(); i++) if (fixed.get(i).equals(name)) return true;
        for (int i = 0; i < bound.size(); i++) if (bound.get(i).equals(name)) return true;
        return false;
    }

    public void addFixed(String names) {
        for (String n : names.trim().split("\\s+")) if (!n.isEmpty() && !fixed.contains(n)) fixed.add(n);
    }

    public boolean setBound(List<String> names) {
        if (bound.equals(names)) return false;
        bound.clear();
        bound.addAll(names);
        return true;
    }

    public boolean toggleFixed(String name, boolean on) {
        boolean present = fixed.contains(name);
        if (on == present) return false;
        if (on) fixed.add(name);
        else fixed.remove(name);
        return true;
    }

    public List<String> all() {
        List<String> out = new ArrayList<>(fixed);
        for (String b : bound) if (!out.contains(b)) out.add(b);
        return out;
    }

    public boolean isEmpty() {
        return fixed.isEmpty() && bound.isEmpty();
    }
}
