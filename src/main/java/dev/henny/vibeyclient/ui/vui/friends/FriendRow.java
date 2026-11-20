package dev.henny.vibeyclient.ui.vui.friends;

import dev.henny.vibeyclient.api.service.FriendsDrawer;
import dev.henny.vibeyclient.ui.vui.HeadTextures;

public final class FriendRow {
    private final FriendsDrawer.Friend f;
    private final String head;

    FriendRow(FriendsDrawer.Friend f) {
        this.f = f;
        this.head = HeadTextures.of(f.userId(), f.skinUrl());
    }

    public String getId() { return f.id(); }
    public String getName() { return f.name(); }
    public String getHead() { return head; }
    public String getStatus() { return f.status(); }
    public String getActivity() { return f.activity() == null ? "" : f.activity(); }
    public boolean isCanInvite() { return f.canInvite(); }
    public boolean isCanJoin() { return f.canJoin(); }

    boolean online() {
        return !"offline".equals(f.status());
    }

    boolean same(FriendsDrawer.Friend other) {
        return f.equals(other) && !head.isEmpty();
    }
}
