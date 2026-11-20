package dev.henny.r3.hint;

public enum Hint {
    HOVER("r3.hint.hover", "{GRAB}: move · {RESIZE}: size"),
    HOLD("r3.hint.hold", "{RESIZE}: size · {DISTANCE_MODIFIER}+{RESIZE}: distance · {MENU}: cancel"),
    PLACE("r3.hint.place", "{USE}: place · {MENU}: cancel · {ROTATE}: turn"),
    EDGE("r3.hint.edge", "{GRAB} or {RESIZE}: resize this edge");

    private final String id;
    private final String template;

    Hint(String id, String template) {
        this.id = id;
        this.template = template;
    }

    public String id() {
        return id;
    }

    public String template() {
        return template;
    }
}
