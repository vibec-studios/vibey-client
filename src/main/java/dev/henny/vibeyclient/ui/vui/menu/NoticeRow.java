package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.backend.Notices;

public final class NoticeRow {
    private final Notices.Notice n;

    NoticeRow(Notices.Notice n) {
        this.n = n;
    }

    public String getId() { return n.id(); }
    public String getTitle() { return n.title(); }
    public String getBody() { return n.body() == null ? n.title() : n.body(); }
    public String getUrl() { return n.url() == null ? "" : n.url(); }
    public String getKind() { return n.level() == Notices.Level.WARN ? "warn" : "info"; }

    Notices.Notice notice() {
        return n;
    }
}
