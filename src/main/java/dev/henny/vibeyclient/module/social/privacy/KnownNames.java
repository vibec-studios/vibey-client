package dev.henny.vibeyclient.module.social.privacy;

import dev.henny.vibeyclient.client.names.Profiles;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.module.social.model.Model;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.LinkedHashSet;
import java.util.Set;

public final class KnownNames {
    private KnownNames() {}

    public static String own() {
        return Minecraft.getInstance().getUser().getName();
    }

    public static Set<String> collect(SocialModule module) {
        Set<String> out = new LinkedHashSet<>();
        out.add(own());
        ClientPacketListener c = Minecraft.getInstance().getConnection();
        if (c != null) {
            for (PlayerInfo p : c.getOnlinePlayers()) {
                String n = Profiles.name(p.getProfile());
                if (n != null) out.add(n);
            }
        }
        if (module.service() != null && module.service().ready()) {
            for (Model.Friend f : module.service().state.sortedFriends()) {
                if (f.user() != null && f.user().name() != null) out.add(f.user().name());
            }
        }
        return out;
    }
}
