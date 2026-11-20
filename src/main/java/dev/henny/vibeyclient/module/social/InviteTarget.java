package dev.henny.vibeyclient.module.social;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.backend.J;
import dev.henny.vibeyclient.module.social.model.Destination;
import dev.henny.vibeyclient.module.social.model.Model.Invite;

public record InviteTarget(String name, String address, int port) {
    public static InviteTarget of(JsonObject connection, Invite invite) {
        String name = invite == null ? null : invite.displayName();
        String address = J.str(connection, "address");
        if (address != null && !address.isBlank()) return new InviteTarget(name, address.trim(), J.num(connection, "port", 25565));
        Destination d = invite == null ? null : invite.destination();
        return d == null ? null : new InviteTarget(name, d.host(), d.port());
    }
}
