package dev.henny.vibeyclient.api.service;

import java.util.List;

public interface FriendsDrawer {
    record Friend(String id, String name, String userId, String skinUrl, String status, String activity, boolean canInvite,
                  boolean canJoin) {}

    String state();

    String name();

    String userId();

    String skinUrl();

    String status();

    void setStatus(String status);

    int requests();

    List<Friend> friends();

    void chat(String friendId);

    void invite(String friendId);

    void join(String friendId);

    void add(String name);

    void openInbox();

    void openAccount();

    void signIn();

    void retry();
}
