package dev.henny.r3.mc;

import java.util.HashMap;
import java.util.Map;

final class HookErrors {
    static final int LOGGED_PER_HOOK = 3;
    private final Map<String, Integer> counts = new HashMap<>();

    void report(String hook, RuntimeException e) {
        int n = counts.merge(hook, 1, Integer::sum);
        if (n <= LOGGED_PER_HOOK) R3Mod.LOG.error("r3 {} hook failed ({}/{} logged)", hook, n, LOGGED_PER_HOOK, e);
    }
}
