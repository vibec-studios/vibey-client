package dev.henny.vibeyclient.api;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Features a server switched off for this client (contract: vibe-launcher {@code docs/PLAY-LINKS.md} §5, channel
 * {@code vibey:rules}). The networking side (server-rules feature) calls {@link #apply}/{@link #clear}; every feature
 * asks {@link #isDisabled} with its key before it acts and registers the keys it understands with {@link #supports}
 * (sent to servers in {@code vibey:hello}).
 *
 * <p>Keys are dotted, e.g. {@code zoom}, {@code freelook}, {@code hud.cps}, {@code hud.coords}, {@code emotes},
 * {@code cosmetics.pets}, {@code discord.server}. A rule may end with a wildcard ({@code hud.*} disables every key
 * starting with {@code hud.}; {@code *} alone disables everything that is supported). Unknown keys are ignored.
 * Thread-safe; listeners run on the thread that applied the rules (the client thread).
 */
public final class ServerRules {
    private ServerRules() {}

    /** Called when the active rules change (joined a server with rules, left it, rules updated). */
    public interface Listener {
        void onRulesChanged();
    }

    private static final Set<String> SUPPORTED = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private record State(List<String> disable, List<String> allow, String message) {}

    private static final State EMPTY = new State(List.of(), List.of(), null);
    private static volatile State rules = EMPTY;

    /** Declares a feature key this client understands (call once at startup, e.g. in {@code onLoad}). */
    public static void supports(String key) {
        SUPPORTED.add(key);
    }

    /** All keys declared via {@link #supports}, for {@code vibey:hello}. */
    public static List<String> supportedKeys() {
        synchronized (SUPPORTED) {
            return List.copyOf(SUPPORTED);
        }
    }

    /** True if the current server disabled this feature key. Cheap; safe to call every frame. */
    public static boolean isDisabled(String key) {
        State current = rules;
        return key != null && matches(current.disable(), key) && !matches(current.allow(), key);
    }

    private static boolean matches(List<String> patterns, String key) {
        for (String rule : patterns) {
            if (rule.equals(key)) return true;
            if (rule.equals("*")) return true;
            if (rule.endsWith(".*") && key.startsWith(rule.substring(0, rule.length() - 1))) return true;
        }
        return false;
    }

    /** The server's explanation ({@code message}), or null. */
    public static String message() {
        return rules.message();
    }

    /** The raw rule list currently active (empty when none). */
    public static List<String> active() {
        return rules.disable();
    }

    /** Sets the rules received from the server (networking side only). Malformed entries are dropped. */
    public static void apply(List<String> disable, String serverMessage) {
        apply(disable, List.of(), serverMessage);
    }

    public static void apply(List<String> disable, List<String> allow, String serverMessage) {
        rules = new State(cleanKeys(disable), cleanKeys(allow),
                serverMessage == null || serverMessage.isBlank() ? null : serverMessage.strip());
        fire();
    }

    private static List<String> cleanKeys(List<String> keys) {
        return keys == null ? List.of() : keys.stream()
                .filter(s -> s != null && !s.isBlank() && s.length() <= 64)
                .filter(s -> s.indexOf('*') < 0 || s.equals("*") || (s.endsWith(".*") && s.indexOf('*') == s.length() - 1))
                .distinct().limit(256).toList();
    }

    /** Clears the rules (left the server). */
    public static void clear() {
        if (rules.equals(EMPTY)) return;
        rules = EMPTY;
        fire();
    }

    public static void listen(Listener l) {
        LISTENERS.add(l);
    }

    public static void unlisten(Listener l) {
        LISTENERS.remove(l);
    }

    private static void fire() {
        for (Listener l : LISTENERS) {
            try {
                l.onRulesChanged();
            } catch (RuntimeException e) {
                dev.henny.vibeyclient.Vibey.LOG.warn("Server rules listener failed", e);
            }
        }
    }
}
