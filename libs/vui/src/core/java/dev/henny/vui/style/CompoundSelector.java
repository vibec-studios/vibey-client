package dev.henny.vui.style;

public record CompoundSelector(String tag, String id, String[] classes, int states, int notStates) {

    public boolean matches(StyleTarget t) {
        if (tag != null && !tag.equals(t.styleTag())) return false;
        if (id != null && !id.equals(t.styleId())) return false;
        for (String c : classes) if (!t.hasClass(c)) return false;
        int s = t.states();
        return (s & states) == states && (s & notStates) == 0;
    }

    public int specificity() {
        int pseudo = Integer.bitCount(states) + Integer.bitCount(notStates);
        return (id != null ? 10000 : 0) + (classes.length + pseudo) * 100 + (tag != null ? 1 : 0);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(tag == null ? "" : tag);
        if (id != null) sb.append('#').append(id);
        for (String c : classes) sb.append('.').append(c);
        sb.append(StateFlags.names(states));
        if (notStates != 0) sb.append(":not(").append(StateFlags.names(notStates)).append(')');
        return sb.length() == 0 ? "*" : sb.toString();
    }
}
