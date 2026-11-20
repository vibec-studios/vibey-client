package dev.henny.vibeyclient.module.nametags;

enum Surface {
    NAMETAG, TAB, CHAT;

    static final int SEAL = 1, RANK = 2, TAG = 4, STYLE = 8, ANIMATE = 16;

    int flags(NametagsModule m) {
        if (m == null || !m.isEnabled() || !shown(m)) return 0;
        int f = 0;
        if (!NametagRules.off(NametagRules.SEAL)) f |= SEAL;
        boolean ranks = !NametagRules.off(NametagRules.RANKS);
        if (ranks && m.ranks.get()) f |= RANK;
        if (ranks && m.badges.get()) f |= TAG;
        if (!NametagRules.off(NametagRules.STYLES) && m.nameStyles.get()) {
            f |= STYLE;
            if (this != CHAT && m.animate.get()) f |= ANIMATE;
        }
        return f;
    }

    private boolean shown(NametagsModule m) {
        return switch (this) {
            case NAMETAG -> m.nametag.get();
            case TAB -> m.tabList.get();
            case CHAT -> m.chat.get();
        };
    }

    static boolean has(int flags, int bit) {
        return (flags & bit) != 0;
    }
}
