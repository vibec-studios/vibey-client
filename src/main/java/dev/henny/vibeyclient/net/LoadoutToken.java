package dev.henny.vibeyclient.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Signed loadouts (VIBE-API-2 §6, SPEC §6): the backend signs {@code {v, kid, uuid, slots, emotes, iat, exp}} with
 * Ed25519; the token is {@code base64url(payloadJson) + "." + base64url(signature over exactly those bytes)}.
 * Receivers verify it with the public key from {@code GET /api/v1/meta} ({@code loadoutSigning}) before they believe
 * the slots and granted emotes another player claims over the server channel.
 * <p>Pure Java (JDK Ed25519 + Gson), no Minecraft classes, so it is unit tested on its own.
 */
public final class LoadoutToken {
    private LoadoutToken() {}

    /** Longest token accepted on the wire (the backend keeps them ≤ 2 KiB). */
    public static final int MAX_LENGTH = 3072;
    /** {@code exp > now − LEEWAY}: clock skew between the backend and the players. */
    public static final long LEEWAY_SECONDS = 5 * 60;
    /** Refresh our own token this long before it expires. */
    public static final long REFRESH_BEFORE_SECONDS = 30 * 60;

    /** Characters of a token: two base64url parts joined by a dot. */
    public static final Pattern WIRE = Pattern.compile("[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]*");

    /** X.509 SubjectPublicKeyInfo header of an Ed25519 key; followed by the 32 raw key bytes. */
    private static final byte[] X509_PREFIX = HexFormat.of().parseHex("302a300506032b6570032100");

    /** The backend's public key ({@code loadoutSigning} of {@code /api/v1/meta}). */
    public record SigningKey(String keyId, byte[] raw, PublicKey key) {
        public String publicKeyBase64() {
            return Base64.getEncoder().encodeToString(raw);
        }
    }

    /** A token whose signature, key id and owner checked out. {@code slots} has no empty values. */
    public record Verified(String kid, UUID uuid, Map<String, String> slots, Set<String> emotes, long iat, long exp) {
        public Verified {
            slots = Collections.unmodifiableMap(new LinkedHashMap<>(slots));
            emotes = Collections.unmodifiableSet(new LinkedHashSet<>(emotes));
        }

        /** Still valid at {@code nowSeconds} (with {@link #LEEWAY_SECONDS}). */
        public boolean live(long nowSeconds) {
            return exp > nowSeconds - LEEWAY_SECONDS;
        }
    }

    /** Why a token was rejected. */
    public enum Failure { MISSING, NO_KEY, MALFORMED, BAD_SIGNATURE, WRONG_KID, WRONG_VERSION, WRONG_UUID, EXPIRED }

    /** Result of {@link #verify}: either {@code token} or {@code failure} is set. */
    public record Result(Verified token, Failure failure) {
        public boolean ok() {
            return token != null;
        }

        static Result fail(Failure f) {
            return new Result(null, f);
        }
    }

    // ============================================================================================ keys

    /** Ed25519 public key from its 32 raw bytes (wrapped into X.509 for the JDK). */
    public static PublicKey publicKey(byte[] raw) throws GeneralSecurityException {
        if (raw == null || raw.length != 32) throw new GeneralSecurityException("Ed25519 public key must be 32 bytes");
        byte[] der = new byte[X509_PREFIX.length + 32];
        System.arraycopy(X509_PREFIX, 0, der, 0, X509_PREFIX.length);
        System.arraycopy(raw, 0, der, X509_PREFIX.length, 32);
        return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
    }

    /** {@code keyId} = first 8 hex characters of SHA-256 over the raw key. */
    public static String keyId(byte[] raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw)).substring(0, 8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Parses {@code loadoutSigning} of {@code /api/v1/meta}. Returns null for {@code null}/absent, an unknown
     * algorithm, a bad key or a {@code keyId} that does not match the key.
     */
    public static SigningKey parseKey(JsonElement e) {
        if (!(e instanceof JsonObject o)) return null;
        if (!(o.get("alg") instanceof JsonPrimitive alg) || !"Ed25519".equalsIgnoreCase(alg.getAsString())) return null;
        if (!(o.get("publicKey") instanceof JsonPrimitive pk) || !(o.get("keyId") instanceof JsonPrimitive kid)) return null;
        try {
            byte[] raw = Base64.getDecoder().decode(pk.getAsString().trim());
            PublicKey key = publicKey(raw);
            String id = keyId(raw);
            if (!id.equalsIgnoreCase(kid.getAsString().trim())) return null;
            return new SigningKey(id, raw, key);
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            return null;
        }
    }

    // ============================================================================================ verify

    /**
     * Checks a token another player sent: signature with {@code key}, {@code kid} = key id, {@code v} = 1,
     * {@code uuid} = {@code sender}, {@code exp > now − 5 min}. Never throws.
     */
    public static Result verify(String token, SigningKey key, UUID sender, long nowSeconds) {
        if (token == null || token.isEmpty()) return Result.fail(Failure.MISSING);
        if (key == null) return Result.fail(Failure.NO_KEY);
        if (token.length() > MAX_LENGTH || !WIRE.matcher(token).matches()) return Result.fail(Failure.MALFORMED);
        int dot = token.indexOf('.');
        byte[] payload, sig;
        try {
            Base64.Decoder d = Base64.getUrlDecoder();
            payload = d.decode(token.substring(0, dot));
            sig = d.decode(token.substring(dot + 1));
        } catch (IllegalArgumentException e) {
            return Result.fail(Failure.MALFORMED);
        }
        if (payload.length == 0 || sig.length != 64) return Result.fail(Failure.MALFORMED);
        try {
            Signature s = Signature.getInstance("Ed25519");
            s.initVerify(key.key());
            s.update(payload);
            if (!s.verify(sig)) return Result.fail(Failure.BAD_SIGNATURE);
        } catch (GeneralSecurityException | RuntimeException e) {
            return Result.fail(Failure.BAD_SIGNATURE);
        }
        Verified v;
        try {
            v = parsePayload(new String(payload, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            return Result.fail(Failure.MALFORMED);
        }
        if (v == null) return Result.fail(Failure.WRONG_VERSION);
        if (!key.keyId().equalsIgnoreCase(v.kid())) return Result.fail(Failure.WRONG_KID);
        if (!v.uuid().equals(sender)) return Result.fail(Failure.WRONG_UUID);
        if (!v.live(nowSeconds)) return Result.fail(Failure.EXPIRED);
        return new Result(v, null);
    }

    /** Reads the payload of our own token (no signature check) for its {@code exp}; null if unreadable. */
    public static Verified peek(String token) {
        if (token == null || token.length() > MAX_LENGTH || !WIRE.matcher(token).matches()) return null;
        try {
            return parsePayload(new String(Base64.getUrlDecoder().decode(token.substring(0, token.indexOf('.'))), StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Payload JSON → {@link Verified} (unchecked); null for another {@code v}. Throws on malformed JSON. */
    static Verified parsePayload(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        if (!(o.get("v") instanceof JsonPrimitive v) || !v.isNumber() || v.getAsInt() != 1) return null;
        String kid = o.get("kid").getAsString();
        UUID uuid = UUID.fromString(o.get("uuid").getAsString());
        long iat = o.get("iat") instanceof JsonPrimitive p && p.isNumber() ? p.getAsLong() : 0;
        long exp = o.get("exp").getAsLong();
        Map<String, String> slots = new LinkedHashMap<>();
        if (o.get("slots") instanceof JsonObject so) {
            for (Map.Entry<String, JsonElement> e : so.entrySet()) {
                if (slots.size() >= NetLimits.MAX_SLOTS) break;
                if (!(e.getValue() instanceof JsonPrimitive p) || !p.isString()) continue; // null = empty slot
                String slot = e.getKey().toLowerCase(Locale.ROOT), id = p.getAsString();
                if (id.isEmpty() || !NetLimits.SLOT.matcher(slot).matches() || !NetLimits.ITEM_ID.matcher(id).matches()) continue;
                slots.put(slot, id);
            }
        }
        Set<String> emotes = new LinkedHashSet<>();
        if (o.get("emotes") instanceof com.google.gson.JsonArray a) {
            for (JsonElement e : a) {
                if (emotes.size() >= 512) break;
                if (e instanceof JsonPrimitive p && p.isString() && !p.getAsString().isEmpty()
                        && NetLimits.ITEM_ID.matcher(p.getAsString()).matches()) emotes.add(p.getAsString());
            }
        }
        return new Verified(kid, uuid, slots, emotes, iat, exp);
    }
}
