package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.servers.friends.FriendItem;

import java.util.function.Function;

import static dev.henny.vibeyclient.gametest.ServerListsKit.check;

final class FieldChecks {
    private FieldChecks() {}

    static void run(Function<String, FriendItem> item, String pingAddress) {
        FriendItem shared = item.apply("s:" + FakeFriends.id('a'));
        check(shared.where().equals("Example SMP") && shared.detail().equals(pingAddress) && shared.canJoin(),
                "shared server: name + address: " + shared);
        FriendItem hidden = item.apply("s:" + FakeFriends.id('b'));
        check(hidden.where().equals("Hidden server") && hidden.detail().isEmpty() && !hidden.canAdd(), "hidden server: " + hidden);
        FriendItem named = item.apply("s:" + FakeFriends.id('e'));
        check(named.where().equals("Secret SMP") && named.detail().equals("Address hidden") && !named.canJoin(),
                "named hidden server: " + named);
        FriendItem world = item.apply("w:" + FakeFriends.id('c'));
        check(world.where().equals("Cherry Valley") && world.canJoin(), "hosted world: " + world);
        FriendItem invite = item.apply("i:inv-2");
        check(invite.where().equals("Example SMP") && invite.detail().equals("play.example.org:25570"), "invite destination: " + invite);
    }
}
