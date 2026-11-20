package dev.henny.vibeyclient.module.nametags;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class AnimatedSet {
    private AnimatedSet() {}

    static final int MAX = 16;
    private static volatile Set<UUID> current = Set.of();

    static boolean contains(UUID id) {
        return current.contains(id);
    }

    static int size() {
        return current.size();
    }

    static void update(int flags) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !Surface.has(flags, Surface.ANIMATE)) {
            current = Set.of();
            return;
        }
        List<Player> list = new ArrayList<>();
        for (Player p : mc.level.players()) if (Decorator.animates(p.getUUID(), flags)) list.add(p);
        Player me = mc.player;
        list.sort(Comparator.comparingDouble(p -> p.distanceToSqr(me)));
        current = Set.copyOf(list.stream().limit(MAX).map(Player::getUUID).toList());
    }
}
