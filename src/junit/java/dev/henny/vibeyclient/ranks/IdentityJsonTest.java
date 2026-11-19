package dev.henny.vibeyclient.ranks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class IdentityJsonTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void readsNewFields() {
        Identity id = IdentityJson.parse(json("{\"client\":\"vibey\",\"badges\":[\"staff\"],\"rank\":\"mod\",\"tags\":[\"beta\"],\"nameStyle\":{\"id\":\"ocean-wave\"}}"));
        assertEquals(new Identity("mod", List.of("beta"), "ocean-wave"), id);
    }

    @Test
    void hiddenRankKeepsStyle() {
        Identity id = IdentityJson.parse(json("{\"badges\":[],\"rank\":null,\"tags\":[],\"nameStyle\":{\"id\":\"rainbow\"}}"));
        assertEquals(new Identity(null, List.of(), "rainbow"), id);
    }

    @Test
    void newBackendWithNothingIsEmpty() {
        assertSame(Identity.EMPTY, IdentityJson.parse(json("{\"badges\":[\"staff\"],\"rank\":null,\"tags\":[],\"nameStyle\":null}")));
    }

    @Test
    void oldBackendFallsBackToBadges() {
        Identity id = IdentityJson.parse(json("{\"client\":\"vibey\",\"badges\":[\"creator\",\"staff\",\"unknown\"]}"));
        assertEquals(new Identity("staff", List.of("creator"), null), id);
    }

    @Test
    void oldPublicUserBadge() {
        assertEquals(new Identity("supporter", List.of(), null), IdentityJson.parse(json("{\"badge\":\"supporter\"}")));
        assertSame(Identity.EMPTY, IdentityJson.parse(json("{\"badge\":\"user\"}")));
    }

    @Test
    void rejectsBadIdsAndDuplicateTags() {
        Identity id = IdentityJson.parse(json("{\"rank\":\"Mod!\",\"tags\":[\"mod\",\"beta\",\"x\"],\"nameStyle\":{\"id\":\"rainbow\"}}"));
        assertEquals(new Identity(null, List.of("mod"), "rainbow"), id);
    }

    @Test
    void ownRanksFromMe() {
        OwnRanks own = OwnRanks.fromMe(json("{\"privacy\":{\"showRank\":false},\"nameStyle\":{\"id\":\"sunset\"},"
                + "\"ranks\":{\"held\":[\"partner\",\"beta\"],\"primary\":\"partner\",\"styles\":[\"sunset\"]}}"));
        assertEquals(new OwnRanks(List.of("partner", "beta"), "partner", List.of("sunset"), false, "sunset"), own);
    }
}
