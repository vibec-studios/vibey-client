package dev.henny.vibeyclient.module.social.activity;

import dev.henny.vibeyclient.module.social.model.Model.Activity;
import dev.henny.vibeyclient.module.social.model.Model.HostSession;
import dev.henny.vibeyclient.module.social.presence.HostPort;

final class HostedJoin {
    private HostedJoin() {}

    static String world(HostSession s, Activity a) {
        if (s.worldName() != null && !s.worldName().isBlank()) return s.worldName().trim();
        return a == null ? null : a.world();
    }

    static String host(HostSession s) {
        if (s.joinHost() != null && !s.joinHost().isBlank()) return s.joinHost().trim();
        HostPort hp = HostPort.parse(s.address());
        return hp == null ? s.address() : hp.host();
    }

    static int port(HostSession s) {
        if (s.joinPort() > 0 && s.joinPort() <= 65535) return s.joinPort();
        HostPort hp = HostPort.parse(s.address());
        return hp == null ? HostPort.DEFAULT : hp.port();
    }
}
