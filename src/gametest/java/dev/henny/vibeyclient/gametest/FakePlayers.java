package dev.henny.vibeyclient.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

final class FakePlayers {
    private FakePlayers() {}

    private static int nextId = -5200;

    static RemotePlayer spawn(Minecraft mc, UUID id, String name, double x, double y, double z, float yaw) throws Exception {
        GameProfile profile = new GameProfile(id, name);
        RemotePlayer p = new RemotePlayer(mc.level, profile);
        p.setPos(x, y, z);
        p.setYRot(yaw);
        p.yRotO = yaw;
        p.yBodyRot = yaw;
        p.yBodyRotO = yaw;
        p.yHeadRot = yaw;
        p.yHeadRotO = yaw;
        p.setId(nextId--);
        mc.level.addEntity(p);
        PlayerInfo info = newInfo(profile);
        infoMap(mc).put(id, info);
        listed(mc).add(info);
        return p;
    }

    static GameProfile profile(UUID id, String name) {
        return new GameProfile(id, name);
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

    private static Object field(Object owner, Class<?> type) throws Exception {
        for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!type.isAssignableFrom(f.getType()) || !(f.getGenericType() instanceof ParameterizedType pt)) continue;
                for (var a : pt.getActualTypeArguments()) {
                    if (a != PlayerInfo.class) continue;
                    f.setAccessible(true);
                    return f.get(owner);
                }
            }
        }
        throw new IllegalStateException("no " + type.getSimpleName() + "<PlayerInfo> field");
    }
}
