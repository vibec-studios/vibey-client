package dev.henny.vibeyclient.module.social;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.module.social.model.Model.Invite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InviteTargetTest {
    static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    static final Invite WITH_DEST = Invite.from(json("{\"id\":\"i\",\"target\":{\"name\":\"T\"},"
            + "\"destination\":{\"host\":\"play.example.org\",\"port\":25570,\"name\":\"Example SMP\"}}"));

    @Test
    void directConnectionFromAccept() {
        InviteTarget t = InviteTarget.of(json("{\"type\":\"direct\",\"address\":\"play.example.org\",\"port\":25570}"), WITH_DEST);
        assertEquals(new InviteTarget("Example SMP", "play.example.org", 25570), t);
    }

    @Test
    void destinationWhenAcceptHasNoConnection() {
        assertEquals(new InviteTarget("Example SMP", "play.example.org", 25570), InviteTarget.of(null, WITH_DEST));
    }

    @Test
    void nothingToJoin() {
        Invite old = Invite.from(json("{\"id\":\"i\",\"target\":{\"name\":\"Farm\"}}"));
        assertNull(InviteTarget.of(null, old));
        assertNull(InviteTarget.of(json("{\"type\":\"relay\",\"address\":null}"), old));
        assertNull(InviteTarget.of(null, null));
    }
}
