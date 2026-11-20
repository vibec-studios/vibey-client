package dev.henny.vibeyclient.module.companion;

import dev.henny.companio.config.CompanioConfig;
import dev.henny.companio.mc.CompanioMc;

final class SettingsBridge {
    private final CompanionModule m;
    private boolean pulling;

    SettingsBridge(CompanionModule m) {
        this.m = m;
    }

    void listen() {
        m.name.onChange(v -> {
            if (!pulling && CompanioMc.ready() && !v.equals(CompanioMc.config().name)) CompanioMc.rename(v);
        });
        m.size.onChange(v -> {
            if (!pulling && CompanioMc.ready()) CompanioMc.size(v);
        });
        m.volume.onChange(v -> push());
        m.bubbles.onChange(v -> push());
        m.others.onChange(v -> push());
        m.share.onChange(v -> push());
    }

    void push() {
        if (!CompanioMc.ready()) return;
        CompanioConfig c = CompanioMc.config();
        c.volume = m.volume.get();
        c.bubbles = m.bubbles.get();
        c.showOthers = m.others.get();
        c.share = m.share.get();
        CompanioMc.changed();
    }

    void pull() {
        CompanioConfig c = CompanioMc.config();
        pulling = true;
        try {
            if (!c.name.equals(m.name.get())) m.name.set(c.name);
            float size = c.size > 0 ? c.size : CompanionChoices.current() == null ? 1f : CompanionChoices.current().size().def();
            if (Math.abs(size - m.size.get()) > 1e-3) m.size.set(size);
        } finally {
            pulling = false;
        }
        if (c.volume != m.volume.get() || c.bubbles != m.bubbles.get() || c.showOthers != m.others.get() || c.share != m.share.get()) push();
    }
}
