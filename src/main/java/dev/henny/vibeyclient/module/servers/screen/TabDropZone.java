package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.module.servers.friends.FriendsTab;
import dev.henny.vui.dom.Element;
import dev.henny.vui.mc.host.DocumentView;

final class TabDropZone implements FriendsTab.DropZone {
    static final String TARGET = "tab-servers";

    private final DocumentView view;
    private final BarController bar;

    TabDropZone(DocumentView view, BarController bar) {
        this.view = view;
        this.bar = bar;
    }

    @Override
    public boolean over(double x, double y) {
        view.document().layoutIfNeeded();
        for (Element e = view.document().events().hit((float) x, (float) y); e != null; e = e.parent()) {
            if (TARGET.equals(e.id())) return true;
        }
        return false;
    }

    @Override
    public void hot(boolean on) {
        bar.hot(on);
    }
}
