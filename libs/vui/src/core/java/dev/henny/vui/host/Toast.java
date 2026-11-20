package dev.henny.vui.host;

public final class Toast {
    private static long counter;

    private final long id = ++counter;
    private final String title;
    private final String text;
    private final String icon;
    private final String kind;
    private float remaining;
    private String action = "";
    private String actionId = "";
    private Runnable onAction;

    public Toast(String title, String text, String icon, String kind, float seconds) {
        this.title = title;
        this.text = text == null ? "" : text;
        this.icon = icon == null ? "" : icon;
        this.kind = kind == null ? "info" : kind;
        this.remaining = seconds;
    }

    public Toast action(String label, Runnable run) {
        return action(label, "", run);
    }

    public Toast action(String label, String id, Runnable run) {
        this.action = label == null ? "" : label;
        this.actionId = id == null ? "" : id;
        this.onAction = run;
        return this;
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

    public String getIconTexture() {
        return icon.indexOf('/') >= 0 ? icon : "";
    }

    public String getIconName() {
        return icon.isEmpty() || icon.indexOf('/') >= 0 ? null : icon;
    }

    public String getKind() {
        return kind;
    }

    public String getAction() {
        return action;
    }

    public String getActionId() {
        return actionId;
    }

    public float remaining() {
        return remaining;
    }

    void run() {
        if (onAction != null) onAction.run();
    }

    boolean tick(float dt) {
        remaining -= dt;
        return remaining <= 0;
    }
}
