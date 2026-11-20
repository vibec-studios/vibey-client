package dev.henny.vibeyclient.api;

public final class StatusCard {
    public enum Kind { OK, WARN, ERROR, OFF }

    private final Kind kind;
    private final String label;
    private final String text;
    private final String detail;

    public StatusCard(Kind kind, String label, String text, String detail) {
        this.kind = kind;
        this.label = label;
        this.text = text == null ? "" : text;
        this.detail = detail == null ? "" : detail;
    }

    public String getKind() { return kind.name().toLowerCase(java.util.Locale.ROOT); }
    public String getLabel() { return label; }
    public String getText() { return text; }
    public String getDetail() { return detail; }
}
