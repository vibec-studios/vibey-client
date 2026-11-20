package dev.henny.vibeyclient.module.cosmetics.selftest;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.registry.Item;

final class SelfTestDevlink {
    private long revision;
    private int idle;
    int shots;

    String newestPush() {
        long rev = 0;
        String id = null;
        for (Item i : Cosmetico.items().all()) {
            if (i.isDev() && i.revision() > rev) {
                rev = i.revision();
                id = i.id();
            }
        }
        if (rev <= revision) return null;
        revision = rev;
        idle = 0;
        return id;
    }

    boolean idleTooLong() {
        return ++idle > 20 * 150;
    }
}
