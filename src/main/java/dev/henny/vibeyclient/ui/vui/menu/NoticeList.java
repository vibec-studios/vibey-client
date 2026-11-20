package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.backend.Notices;
import dev.henny.vibeyclient.ui.notice.Announcements;
import dev.henny.vui.bind.ListState;

import java.util.ArrayList;
import java.util.List;

public final class NoticeList {
    public final ListState<NoticeRow> notices = new ListState<>();
    private List<Notices.Notice> last = List.of();

    public void sync() {
        List<Notices.Notice> now = Announcements.active();
        if (now.equals(last)) return;
        last = List.copyOf(now);
        List<NoticeRow> rows = new ArrayList<>();
        for (Notices.Notice n : last) rows.add(new NoticeRow(n));
        notices.replaceAll(rows);
    }

    public void open(NoticeRow n) {
        Announcements.open(n.notice());
    }

    public void dismiss(NoticeRow n) {
        Announcements.dismiss(n.getId());
        sync();
    }
}
