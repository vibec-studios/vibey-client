package dev.henny.vibeyclient.module.servers.friends;

public record FriendItem(String key, String kind, String title, int count, String ref, String name, String head,
                         String rank, String rankColor, String icon, String where, String detail, String ping, boolean bad,
                         boolean canJoin, boolean canAdd, boolean canDecline, String address, String status,
                         String card) {
    public boolean header() {
        return "header".equals(kind);
    }

    static FriendItem header(String key, String title, int count) {
        return new FriendItem(key, "header", title, count, "", "", "", "", "", "", "", "", "", false, false, false, false,
                "", "", "");
    }
}
