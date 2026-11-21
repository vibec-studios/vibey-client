package dev.henny.vibeyclient.access;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessInfoTest {
    private static AccessInfo parse(String json) {
        return AccessInfo.parse(JsonParser.parseString(json));
    }

    @Test
    void contractShape() {
        AccessInfo w = parse("{\"status\":\"waitlist\",\"position\":12,\"total\":40,\"requestedAt\":\"2026-11-20T10:00:00Z\","
                + "\"decidedAt\":null,\"reason\":null,\"mode\":\"waitlist\"}");
        assertEquals(AccessStatus.WAITLIST, w.status());
        assertEquals(12, w.position());
        assertEquals(40, w.total());
        assertTrue(parse("{\"status\":\"approved\",\"position\":null}").approved());
        AccessInfo r = parse("{\"status\":\"rejected\",\"reason\":\"  Doppeltes Konto \"}");
        assertEquals("Doppeltes Konto", r.reason());
    }

    @Test
    void positionOnlyOnTheWaitlistAndUnknownMeansClosed() {
        assertNull(parse("{\"status\":\"approved\",\"position\":3}").position());
        assertNull(parse("{\"status\":\"waitlist\",\"position\":0}").position());
        assertNull(parse("{\"status\":\"waitlist\",\"position\":\"x\"}").position());
        assertEquals(AccessStatus.NONE, parse("{\"status\":\"vip\"}").status());
        assertEquals(AccessStatus.NONE, parse("[]").status());
        assertFalse(AccessInfo.parse(null).approved());
    }

    @Test
    void denialCodes() {
        assertTrue(AccessStatus.isDenial("ACCESS_PENDING"));
        assertFalse(AccessStatus.isDenial("banned"));
        assertEquals(AccessStatus.WAITLIST, AccessStatus.fromDenial("ACCESS_PENDING"));
        assertEquals(AccessStatus.REJECTED, AccessStatus.fromDenial("ACCESS_REJECTED"));
        assertEquals(AccessStatus.NONE, AccessStatus.fromDenial("ACCESS_NONE"));
        assertEquals(AccessStatus.NONE, AccessStatus.fromDenial(null));
    }
}
