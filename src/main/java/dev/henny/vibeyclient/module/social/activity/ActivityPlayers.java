package dev.henny.vibeyclient.module.social.activity;

import dev.henny.vibeyclient.api.service.FriendsActivity.Player;
import dev.henny.vibeyclient.module.social.model.Model.Presence;
import dev.henny.vibeyclient.module.social.model.Model.User;

final class ActivityPlayers {
    private ActivityPlayers() {}

    static Player of(User u, Presence p) {
        String rank = u.identity() == null ? null : u.identity().rank();
        String status = p == null || p.status() == null ? "offline" : p.status();
        return new Player(u.id(), u.name(), u.skinUrl(), status, rank);
    }
}
