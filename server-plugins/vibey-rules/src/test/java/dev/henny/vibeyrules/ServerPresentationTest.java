package dev.henny.vibeyrules;

import dev.henny.vibeyclient.net.ServerPresentation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerPresentationTest {
    @Test
    void presentationRoundTripsAndIndividualColourOverridesTeam() {
        UUID player = UUID.randomUUID();
        var waypoint = new ServerPresentation.Waypoint("spawn", "Treffpunkt", "minecraft:overworld", 12, 64, -16, 0x0055FF);
        var team = new ServerPresentation.Team("blue", "Blau", 0x0055FF, List.of(player));
        var presentation = new ServerPresentation(List.of(waypoint), List.of(team), Map.of(player, 0x00FFFF));
        assertEquals(presentation, ServerPresentation.decode(presentation.encode()));
        assertEquals(Map.of(player, 0x00FFFF), presentation.resolvedColors());
        assertEquals(ServerPresentation.EMPTY, ServerPresentation.decode(bytes("{\"schema\":1}")));
    }

    @Test
    void rejectsOversizedPacketsAndAmbiguousMembers() {
        assertThrows(IllegalArgumentException.class, () -> ServerPresentation.decode(new byte[ServerPresentation.MAX_BYTES + 1]));
        UUID player = UUID.randomUUID();
        var first = new ServerPresentation.Team("one", "One", 0, List.of(player));
        var second = new ServerPresentation.Team("two", "Two", 1, List.of(player));
        assertThrows(IllegalArgumentException.class, () -> new ServerPresentation(List.of(), List.of(first, second), Map.of()));
    }

    @Test
    void rejectsMalformedSnapshots() {
        for (String invalid : List.of("{\"schema\":2}", "{\"schema\":1,\"waypoints\":[{}]}",
                "{\"schema\":1,\"teams\":{}}", "{\"schema\":1,\"nametagColors\":{\"1-1-1-1-1\":0}}")) {
            assertThrows(IllegalArgumentException.class, () -> ServerPresentation.decode(bytes(invalid)));
        }
    }

    private static byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
