package dev.henny.vibeyclient.dev;

import com.mojang.authlib.GameProfile;
import dev.henny.vibeyclient.client.VibeyPlayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dev tours only: puts two fake players into the client world and the tab list so the nametag icons can be looked
 * at on every version without a second client. "VibeyFriend" runs Vibey Client and has the staff + creator badges,
 * "PlainSteve" does not. Tab list entries are added to the connection's player list by reflection (by field type,
 * so it works with every version's names).
 */
final class NametagProbe {
    private NametagProbe() {}

    static final UUID FRIEND = UUID.fromString("5ea10000-0000-4000-8000-00000000f00d");
    static final UUID PLAIN = UUID.fromString("5ea10000-0000-4000-8000-00000000beef");
    private static final List<RemotePlayer> SPAWNED = new ArrayList<>();
    private static final List<PlayerInfo> LISTED = new ArrayList<>();

    /** Spawns both players {@code distance} blocks in front of the local player (looking south), facing it. */
    static void spawn(Minecraft mc, double distance) throws Exception {
        remove(mc);
        VibeyPlayers.backend(FRIEND, true, List.of("staff", "creator"));
        VibeyPlayers.backend(PLAIN, false, List.of());
        add(mc, FRIEND, "VibeyFriend", -1.4, distance);
        add(mc, PLAIN, "PlainSteve", 1.4, distance);
    }

    private static void add(Minecraft mc, UUID id, String name, double dx, double dz) throws Exception {
        GameProfile profile = new GameProfile(id, name);
        RemotePlayer p = new RemotePlayer(mc.level, profile);
        p.setPos(mc.player.getX() + dx, mc.player.getY(), mc.player.getZ() + dz);
        float yaw = 180f;
        p.setYRot(yaw);
        p.yRotO = yaw;
        p.yBodyRot = yaw;
        p.yBodyRotO = yaw;
        p.yHeadRot = yaw;
        p.yHeadRotO = yaw;
        mc.level.addEntity(p);
        SPAWNED.add(p);
        PlayerInfo info = newInfo(profile);
        infoMap(mc).put(id, info);
        listed(mc).add(info);
        LISTED.add(info);
    }

    static void remove(Minecraft mc) {
        for (RemotePlayer p : SPAWNED) {
            if (mc.level != null) mc.level.removeEntity(p.getId(), Entity.RemovalReason.DISCARDED);
        }
        SPAWNED.clear();
        try {
            if (mc.getConnection() != null) {
                for (PlayerInfo i : LISTED) listed(mc).remove(i);
                infoMap(mc).remove(FRIEND);
                infoMap(mc).remove(PLAIN);
            }
        } catch (Exception ignored) {
        }
        LISTED.clear();
    }

    private static PlayerInfo newInfo(GameProfile profile) throws Exception {
        for (Constructor<?> c : PlayerInfo.class.getDeclaredConstructors()) {
            Class<?>[] t = c.getParameterTypes();
            if (t.length == 2 && t[0] == GameProfile.class && t[1] == boolean.class) {
                c.setAccessible(true);
                return (PlayerInfo) c.newInstance(profile, false);
            }
        }
        throw new IllegalStateException("no PlayerInfo(GameProfile, boolean)");
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, PlayerInfo> infoMap(Minecraft mc) throws Exception {
        return (Map<UUID, PlayerInfo>) field(mc.getConnection(), Map.class);
    }

    @SuppressWarnings("unchecked")
    private static Collection<PlayerInfo> listed(Minecraft mc) throws Exception {
        return (Collection<PlayerInfo>) field(mc.getConnection(), java.util.Set.class);
    }

    /** The first field of {@code type} whose generic arguments mention PlayerInfo. */
    private static Object field(Object owner, Class<?> type) throws Exception {
        for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!type.isAssignableFrom(f.getType())) continue;
                if (!(f.getGenericType() instanceof ParameterizedType pt)) continue;
                boolean info = false;
                for (var a : pt.getActualTypeArguments()) if (a == PlayerInfo.class) info = true;
                if (!info) continue;
                f.setAccessible(true);
                return f.get(owner);
            }
        }
        throw new IllegalStateException("no " + type.getSimpleName() + "<PlayerInfo> field in " + owner.getClass().getName());
    }
}
