package dev.henny.vibeyclient.module.servers.core;

import dev.henny.vibeyclient.api.service.FriendsActivity.HostedWorld;
import dev.henny.vibeyclient.api.service.FriendsActivity.Invite;
import dev.henny.vibeyclient.api.service.FriendsActivity.OnServer;

public final class FriendLabels {
    public record Label(String title, String detail) {
        public boolean hiddenTitle() {
            return title == null;
        }

        public boolean hiddenDetail() {
            return detail == null;
        }
    }

    private FriendLabels() {}

    public static Label server(OnServer s) {
        String name = clean(s.server());
        if (s.shared()) {
            String address = s.address().trim();
            if (name == null || name.equalsIgnoreCase(address)) return new Label(address, "");
            return new Label(name, address);
        }
        return new Label(name, name == null ? "" : null);
    }

    public static Label world(HostedWorld w) {
        return new Label(clean(w.world()), "");
    }

    public static Label invite(Invite i) {
        String address = i.address();
        String name = clean(i.target());
        if (address == null) return new Label(name, "");
        if (name == null) return new Label(address, "");
        return new Label(name, name.equalsIgnoreCase(address) ? "" : address);
    }

    private static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
