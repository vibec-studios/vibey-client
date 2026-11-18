package dev.henny.vibeyclient.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Signed loadouts (VIBE-API-2 §6, SPEC §6) with a key pair generated here, plus the loadout packet codec. */
class LoadoutTokenTest {
    static final UUID ALICE = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    static final UUID BOB = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    static final long NOW = 1_800_000_000L;

    static KeyPair pair;
    static byte[] raw;
    static LoadoutToken.SigningKey key;

    @BeforeAll
    static void keys() throws Exception {
        pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        byte[] x509 = pair.getPublic().getEncoded();
        raw = Arrays.copyOfRange(x509, x509.length - 32, x509.length);
        key = LoadoutToken.parseKey(meta(raw, LoadoutToken.keyId(raw)));
        assertNotNull(key);
    }

    static JsonObject meta(byte[] rawKey, String kid) {
        JsonObject o = new JsonObject();
        o.addProperty("alg", "Ed25519");
        o.addProperty("keyId", kid);
        o.addProperty("publicKey", Base64.getEncoder().encodeToString(rawKey));
        return o;
    }

    static JsonObject payload(String kid, UUID uuid, long iat, long exp) {
        JsonObject p = new JsonObject();
        p.addProperty("v", 1);
        p.addProperty("kid", kid);
        p.addProperty("uuid", uuid.toString());
        JsonObject slots = new JsonObject();
        slots.addProperty("wings", "vibe:neon_wings");
        slots.addProperty("cape", "vibe:seal_cape");
        slots.add("hat", JsonNull.INSTANCE);
        p.add("slots", slots);
        JsonArray emotes = new JsonArray();
        emotes.add("vibe:seal_summon");
        p.add("emotes", emotes);
        p.addProperty("iat", iat);
        p.addProperty("exp", exp);
        return p;
    }

    static String sign(byte[] payloadBytes, PrivateKey priv) throws Exception {
        Signature s = Signature.getInstance("Ed25519");
        s.initSign(priv);
        s.update(payloadBytes);
        Base64.Encoder e = Base64.getUrlEncoder().withoutPadding();
        return e.encodeToString(payloadBytes) + "." + e.encodeToString(s.sign());
    }

    static String token(JsonObject payload) throws Exception {
        return sign(payload.toString().getBytes(StandardCharsets.UTF_8), pair.getPrivate());
    }

    static String good() throws Exception {
        return token(payload(key.keyId(), ALICE, NOW - 60, NOW - 60 + 6 * 3600));
    }

    // ============================================================================================ keys

    @Test
    void keyIdIsSha256Prefix() {
        assertEquals(8, key.keyId().length());
        assertTrue(key.keyId().matches("[0-9a-f]{8}"));
        assertArrayEquals(raw, key.raw());
        assertEquals(Base64.getEncoder().encodeToString(raw), key.publicKeyBase64());
    }

    @Test
    void rejectsBadMetaKeys() {
        assertNull(LoadoutToken.parseKey(JsonNull.INSTANCE));
        assertNull(LoadoutToken.parseKey(null));
        assertNull(LoadoutToken.parseKey(meta(raw, "deadbeef".equals(key.keyId()) ? "cafebabe" : "deadbeef")));
        assertNull(LoadoutToken.parseKey(meta(Arrays.copyOf(raw, 31), key.keyId())));
        JsonObject rsa = meta(raw, key.keyId());
        rsa.addProperty("alg", "RS256");
        assertNull(LoadoutToken.parseKey(rsa));
        // key id is compared case-insensitively
        assertNotNull(LoadoutToken.parseKey(meta(raw, key.keyId().toUpperCase())));
    }

    // ============================================================================================ verify

    @Test
    void validToken() throws Exception {
        LoadoutToken.Result r = LoadoutToken.verify(good(), key, ALICE, NOW);
        assertTrue(r.ok(), String.valueOf(r.failure()));
        assertEquals(ALICE, r.token().uuid());
        assertEquals(Map.of("wings", "vibe:neon_wings", "cape", "vibe:seal_cape"), r.token().slots()); // null slot dropped
        assertEquals(List.of("vibe:seal_summon"), List.copyOf(r.token().emotes()));
        assertEquals(NOW - 60 + 6 * 3600, r.token().exp());
    }

    @Test
    void missingTokenOrKey() throws Exception {
        assertEquals(LoadoutToken.Failure.MISSING, LoadoutToken.verify("", key, ALICE, NOW).failure());
        assertEquals(LoadoutToken.Failure.MISSING, LoadoutToken.verify(null, key, ALICE, NOW).failure());
        assertEquals(LoadoutToken.Failure.NO_KEY, LoadoutToken.verify(good(), null, ALICE, NOW).failure());
    }

    @Test
    void tamperedPayload() throws Exception {
        String t = good();
        String sig = t.substring(t.indexOf('.') + 1);
        JsonObject p = JsonParser.parseString(new String(Base64.getUrlDecoder().decode(t.substring(0, t.indexOf('.'))), StandardCharsets.UTF_8)).getAsJsonObject();
        p.getAsJsonObject("slots").addProperty("aura", "vibe:staff_only_aura");
        String forged = Base64.getUrlEncoder().withoutPadding().encodeToString(p.toString().getBytes(StandardCharsets.UTF_8)) + "." + sig;
        assertEquals(LoadoutToken.Failure.BAD_SIGNATURE, LoadoutToken.verify(forged, key, ALICE, NOW).failure());
        // the same bytes re-serialised with different whitespace are different bytes: the signature covers bytes
        String spaced = Base64.getUrlEncoder().withoutPadding().encodeToString(
                (" " + new String(Base64.getUrlDecoder().decode(t.substring(0, t.indexOf('.'))), StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8)) + "." + sig;
        assertEquals(LoadoutToken.Failure.BAD_SIGNATURE, LoadoutToken.verify(spaced, key, ALICE, NOW).failure());
    }

    @Test
    void tamperedSignatureAndGarbage() throws Exception {
        String t = good();
        char last = t.charAt(t.length() - 5);
        String flipped = t.substring(0, t.length() - 5) + (last == 'A' ? 'B' : 'A') + t.substring(t.length() - 4);
        assertFalse(LoadoutToken.verify(flipped, key, ALICE, NOW).ok());
        assertEquals(LoadoutToken.Failure.MALFORMED, LoadoutToken.verify("not-a-token", key, ALICE, NOW).failure());
        assertEquals(LoadoutToken.Failure.MALFORMED, LoadoutToken.verify("a.b.c", key, ALICE, NOW).failure());
        assertEquals(LoadoutToken.Failure.MALFORMED, LoadoutToken.verify(t.substring(0, t.indexOf('.')) + ".", key, ALICE, NOW).failure());
        assertEquals(LoadoutToken.Failure.MALFORMED, LoadoutToken.verify("x".repeat(LoadoutToken.MAX_LENGTH) + ".y", key, ALICE, NOW).failure());
        // validly signed, but not JSON
        assertEquals(LoadoutToken.Failure.MALFORMED,
                LoadoutToken.verify(sign("hello".getBytes(StandardCharsets.UTF_8), pair.getPrivate()), key, ALICE, NOW).failure());
    }

    @Test
    void signedByAnotherKey() throws Exception {
        KeyPair other = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String t = sign(payload(key.keyId(), ALICE, NOW, NOW + 3600).toString().getBytes(StandardCharsets.UTF_8), other.getPrivate());
        assertEquals(LoadoutToken.Failure.BAD_SIGNATURE, LoadoutToken.verify(t, key, ALICE, NOW).failure());
    }

    @Test
    void wrongUuid() throws Exception {
        // Bob replays Alice's valid token
        assertEquals(LoadoutToken.Failure.WRONG_UUID, LoadoutToken.verify(good(), key, BOB, NOW).failure());
    }

    @Test
    void expired() throws Exception {
        long exp = NOW - LoadoutToken.LEEWAY_SECONDS - 1;
        String old = token(payload(key.keyId(), ALICE, exp - 6 * 3600, exp));
        assertEquals(LoadoutToken.Failure.EXPIRED, LoadoutToken.verify(old, key, ALICE, NOW).failure());
        // within the 5 minute leeway it still counts
        String recent = token(payload(key.keyId(), ALICE, NOW - 6 * 3600, NOW - 60));
        assertTrue(LoadoutToken.verify(recent, key, ALICE, NOW).ok());
    }

    @Test
    void wrongKid() throws Exception {
        String t = token(payload(key.keyId().equals("00000000") ? "11111111" : "00000000", ALICE, NOW, NOW + 3600));
        assertEquals(LoadoutToken.Failure.WRONG_KID, LoadoutToken.verify(t, key, ALICE, NOW).failure());
    }

    @Test
    void unknownVersion() throws Exception {
        JsonObject p = payload(key.keyId(), ALICE, NOW, NOW + 3600);
        p.addProperty("v", 2);
        assertEquals(LoadoutToken.Failure.WRONG_VERSION, LoadoutToken.verify(token(p), key, ALICE, NOW).failure());
    }

    @Test
    void badSlotEntriesAreDropped() throws Exception {
        JsonObject p = payload(key.keyId(), ALICE, NOW, NOW + 3600);
        p.getAsJsonObject("slots").addProperty("NOT A SLOT!", "vibe:x");
        p.getAsJsonObject("slots").addProperty("back", "Bad Id");
        LoadoutToken.Result r = LoadoutToken.verify(token(p), key, ALICE, NOW);
        assertTrue(r.ok());
        assertEquals(2, r.token().slots().size());
    }

    @Test
    void peekReadsOwnTokenWithoutKey() throws Exception {
        LoadoutToken.Verified v = LoadoutToken.peek(good());
        assertNotNull(v);
        assertEquals(ALICE, v.uuid());
        assertNull(LoadoutToken.peek("garbage"));
    }

    // ============================================================================================ codec

    static LoadoutPayload roundTrip(LoadoutPayload p) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            LoadoutPayload.write(buf, p);
            LoadoutPayload back = LoadoutPayload.read(buf);
            assertEquals(0, buf.readableBytes(), "decoder must consume the whole packet");
            return back;
        } finally {
            buf.release();
        }
    }

    @Test
    void codecWithAndWithoutToken() throws Exception {
        Map<String, String> slots = new LinkedHashMap<>();
        slots.put("wings", "vibe:neon_wings");
        slots.put("hat", "");
        String t = good();
        LoadoutPayload with = roundTrip(new LoadoutPayload(ALICE, slots, t));
        assertEquals(t, with.token());
        assertEquals(slots, with.slots());
        LoadoutPayload without = roundTrip(new LoadoutPayload(ALICE, slots));
        assertEquals("", without.token());
        assertEquals(slots, without.slots());
    }

    @Test
    void tokenlessPacketIsProtocol1Format() {
        // protocol 1 wrote exactly: uuid, varint n, n × (slot, id). A tokenless packet must be byte-identical.
        Map<String, String> slots = Map.of("cape", "vibe:seal_cape");
        FriendlyByteBuf v2 = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf v1 = new FriendlyByteBuf(Unpooled.buffer());
        try {
            LoadoutPayload.write(v2, new LoadoutPayload(BOB, slots, ""));
            v1.writeUUID(BOB);
            v1.writeVarInt(1);
            v1.writeUtf("cape", NetLimits.MAX_SLOT_NAME);
            v1.writeUtf("vibe:seal_cape", NetLimits.MAX_ITEM_ID);
            assertArrayEquals(io.netty.buffer.ByteBufUtil.getBytes(v1), io.netty.buffer.ByteBufUtil.getBytes(v2));
            // and a protocol 1 packet decodes with an empty token
            LoadoutPayload p = LoadoutPayload.read(v1);
            assertEquals("", p.token());
            assertEquals(slots, p.slots());
        } finally {
            v1.release();
            v2.release();
        }
    }

    @Test
    void withoutTokenForOldPeers() throws Exception {
        LoadoutPayload p = new LoadoutPayload(ALICE, Map.of("cape", "vibe:seal_cape"), good());
        LoadoutPayload stripped = p.withoutToken();
        assertEquals("", stripped.token());
        assertEquals(p.slots(), stripped.slots());
        assertEquals(p.player(), stripped.player());
    }
}
