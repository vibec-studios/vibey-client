package dev.henny.vibeyclient.module.servers.friends;

final class FriendCard {
    static final float WIDTH = 170, HEIGHT = 34;

    private final FriendsController c;
    private FriendItem shown;

    FriendCard(FriendsController c) {
        this.c = c;
    }

    void update(FriendItem item, int mouseX, int mouseY, int screenW, int screenH) {
        if (item == null) {
            shown = null;
            if (c.cardOpen.peek()) c.cardOpen.set(false);
            return;
        }
        if (shown == null || !shown.key().equals(item.key()) || !shown.card().equals(item.card())) {
            c.cardName.set(item.name());
            c.cardHead.set(item.head());
            c.cardText.set(item.card());
            shown = item;
        }
        float x = Math.min(mouseX + 12, screenW - WIDTH - 4);
        float y = mouseY + 12 + HEIGHT > screenH - 4 ? mouseY - HEIGHT - 6 : mouseY + 12;
        if (c.cardX.peek() != x) c.cardX.set(x);
        if (c.cardY.peek() != y) c.cardY.set(y);
        if (!c.cardOpen.peek()) c.cardOpen.set(true);
    }
}
