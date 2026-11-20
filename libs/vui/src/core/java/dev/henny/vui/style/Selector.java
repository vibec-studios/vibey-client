package dev.henny.vui.style;

public final class Selector {
    public static final char DESCENDANT = ' ';
    public static final char CHILD = '>';

    private final CompoundSelector[] parts;
    private final char[] combinators;
    private final int specificity;
    private final String source;

    public Selector(CompoundSelector[] parts, char[] combinators, String source) {
        this.parts = parts;
        this.combinators = combinators;
        this.source = source;
        int s = 0;
        for (CompoundSelector p : parts) s += p.specificity();
        this.specificity = s;
    }

    public int specificity() {
        return specificity;
    }

    public CompoundSelector subject() {
        return parts[parts.length - 1];
    }

    public int stateMaskOfAncestors() {
        int mask = 0;
        for (int i = 0; i < parts.length - 1; i++) mask |= parts[i].states() | parts[i].notStates();
        return mask;
    }

    public int stateMaskOfSubject() {
        return subject().states() | subject().notStates();
    }

    public boolean matches(StyleTarget target) {
        int last = parts.length - 1;
        if (!parts[last].matches(target)) return false;
        return matchFrom(last - 1, target);
    }

    private boolean matchFrom(int index, StyleTarget current) {
        if (index < 0) return true;
        char combinator = combinators[index];
        StyleTarget ancestor = current.styleParent();
        if (combinator == CHILD) {
            return ancestor != null && parts[index].matches(ancestor) && matchFrom(index - 1, ancestor);
        }
        while (ancestor != null) {
            if (parts[index].matches(ancestor) && matchFrom(index - 1, ancestor)) return true;
            ancestor = ancestor.styleParent();
        }
        return false;
    }

    @Override
    public String toString() {
        return source;
    }
}
