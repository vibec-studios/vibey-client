package dev.henny.vui.host;

public final class Toast {
    private static long counter;

    private final long id = ++counter;
    private final String title;
    private final String text;
    private final String icon;
    private final String kind;
    private float remaining;

    public Toast(String title, String text, String icon, String kind, float seconds) {
        this.title = title;
        this.text = text == null ? "" : text;
        this.icon = icon == null ? "" : icon;
        this.kind = kind == null ? "info" : kind;
        this.remaining = seconds;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getIcon() {
        return icon;
    }

    public String getKind() {
        return kind;
    }

    public float remaining() {
        return remaining;
    }

    boolean tick(float dt) {
        remaining -= dt;
        return remaining <= 0;
    }
}
