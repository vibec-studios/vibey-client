package dev.henny.vibeyclient.module.hud.tab;

import dev.henny.vibeyclient.client.GameCompat;
import dev.henny.vibeyclient.client.hud.HudAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
//? if >=1.21.9 {
import net.minecraft.world.entity.player.PlayerSkin;
//?} else
/*import net.minecraft.client.resources.PlayerSkin;*/
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class TabRows {
    static final int MAX = 80;
    private static final Comparator<PlayerInfo> VANILLA = Comparator.comparingInt(TabRows::order)
            .thenComparingInt(p -> p.getGameMode() == GameType.SPECTATOR ? 1 : 0)
            .thenComparing(p -> p.getTeam() == null ? "" : p.getTeam().getName())
            .thenComparing(p -> GameCompat.profileName(p.getProfile()), String::compareToIgnoreCase);
    private static final Comparator<PlayerInfo> NAME = Comparator.comparing(p -> GameCompat.profileName(p.getProfile()), String::compareToIgnoreCase);
    private static final Comparator<PlayerInfo> PING = Comparator.comparingInt(PlayerInfo::getLatency).thenComparing(NAME);

    private static int order(PlayerInfo p) {
        //? if >=1.21.2 {
        return -p.getTabListOrder();
        //?} else
        /*return 0;*/
    }

    final List<Component> names = new ArrayList<>();
    final List<PlayerSkin> skins = new ArrayList<>();
    final List<Integer> pings = new ArrayList<>();
    final List<String> labels = new ArrayList<>();

    void read(String sort, boolean preview) {
        names.clear();
        skins.clear();
        pings.clear();
        labels.clear();
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener c = mc.getConnection();
        if (c != null) {
            List<PlayerInfo> list = new ArrayList<>(c.getListedOnlinePlayers());
            list.sort("name".equals(sort) ? NAME : "ping".equals(sort) ? PING : VANILLA);
            for (int i = 0; i < Math.min(MAX, list.size()); i++) {
                PlayerInfo p = list.get(i);
                names.add(HudAccess.tabList().getNameForDisplay(p));
                skins.add(p.getSkin());
                pings.add(p.getLatency());
                labels.add(p.getLatency() + "ms");
            }
        }
        if (preview && names.size() < 3) samples();
    }

    private void samples() {
        String[] n = {"Alex", "Steve", "Kai", "Noor"};
        int[] ping = {24, 61, 140, 310};
        for (int i = 0; i < n.length; i++) {
            names.add(Component.literal(n[i]));
            skins.add(DefaultPlayerSkin.get(UUID.nameUUIDFromBytes(n[i].getBytes())));
            pings.add(ping[i]);
            labels.add(ping[i] + "ms");
        }
    }
}
