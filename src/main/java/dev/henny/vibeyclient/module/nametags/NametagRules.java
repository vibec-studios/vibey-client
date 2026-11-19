package dev.henny.vibeyclient.module.nametags;

import dev.henny.vibeyclient.api.ServerRules;

final class NametagRules {
    private NametagRules() {}

    static final String SEAL = "nametags.seal";
    static final String RANKS = "nametags.ranks";
    static final String STYLES = "nametags.styles";

    static void declare() {
        ServerRules.supports(SEAL);
        ServerRules.supports(RANKS);
        ServerRules.supports(STYLES);
    }

    static boolean off(String key) {
        return ServerRules.isDisabled(key);
    }
}
