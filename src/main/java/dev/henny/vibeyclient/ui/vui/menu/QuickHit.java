package dev.henny.vibeyclient.ui.vui.menu;

public final class QuickHit {
    private final String id;
    private final String label;
    private final boolean more;

    QuickHit(String id, String label, boolean more) {
        this.id = id;
        this.label = label;
        this.more = more;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public boolean isMore() { return more; }
}
