package dev.henny.vibeafk.game;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class GameBridge {
    public static final String MINECRAFT = "net.minecraft.client.Minecraft";
    static final String LOCAL_PLAYER = "net.minecraft.client.player.LocalPlayer";
    static final String PLAYER = "net.minecraft.world.entity.player.Player";
    static final String LIVING = "net.minecraft.world.entity.LivingEntity";
    static final String FOOD = "net.minecraft.world.food.FoodData";
    static final String PACKET_LISTENER = "net.minecraft.client.multiplayer.ClientPacketListener";
    static final String SERVER_DATA = "net.minecraft.client.multiplayer.ServerData";
    static final String SERVER_ADDRESS = "net.minecraft.client.multiplayer.resolver.ServerAddress";
    static final String CONNECT = "net.minecraft.client.gui.screens.ConnectScreen";
    static final String DISCONNECTED = "net.minecraft.client.gui.screens.DisconnectedScreen";
    static final String DETAILS = "net.minecraft.network.DisconnectionDetails";
    static final String DEATH = "net.minecraft.client.gui.screens.DeathScreen";
    static final String SCREEN = "net.minecraft.client.gui.screens.Screen";
    static final String USER = "net.minecraft.client.User";
    static final String GUI = "net.minecraft.client.gui.Gui";

    public static final Set<String> MEMBER_CLASSES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            MINECRAFT, LOCAL_PLAYER, "net.minecraft.client.player.AbstractClientPlayer", PLAYER, LIVING,
            "net.minecraft.world.entity.Entity", FOOD, PACKET_LISTENER,
            "net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl", SERVER_DATA, SERVER_ADDRESS, CONNECT,
            DISCONNECTED, DETAILS, DEATH, SCREEN, USER, GUI)));

    public enum ScreenKind { LOADING, MENU, WORLD, DEATH, DISCONNECTED, CONNECTING, UNKNOWN }

    private final Reflect r;
    private final Method getInstance;
    private final Field player;
    private final Field level;
    private final Field screen;
    private final Field gui;
    private Method guiScreen;
    private final Method currentServer;
    private final Method getConnection;
    private final Method getUser;
    private final Class<?> deathCls;
    private final Class<?> disconnectedCls;
    private final Class<?> connectCls;
    private final Class<?> serverDataCls;
    private Field serverIp;
    private Method userName;
    private Method health;
    private Method maxHealth;
    private Method foodData;
    private Method foodLevel;
    private final Method startConnecting;
    private final Method parseAddress;

    private GameBridge(Reflect r, Class<?> mc) {
        this.r = r;
        getInstance = staticNoArg(r.method(mc, "getInstance", 0));
        player = r.field(mc, "player");
        level = r.field(mc, "level");
        screen = r.field(mc, "screen");
        gui = screen == null ? r.field(mc, "gui") : null;
        currentServer = r.method(mc, "getCurrentServer", 0);
        getConnection = r.method(mc, "getConnection", 0);
        getUser = r.method(mc, "getUser", 0);
        deathCls = r.cls(DEATH);
        disconnectedCls = r.cls(DISCONNECTED);
        connectCls = r.cls(CONNECT);
        serverDataCls = r.cls(SERVER_DATA);
        if (serverDataCls != null) serverIp = r.field(serverDataCls, "ip");
        Class<?> user = r.cls(USER);
        if (user != null) userName = r.method(user, "getName", 0);
        startConnecting = connectCls == null ? null : staticAny(r.method(connectCls, "startConnecting", -1));
        Class<?> addr = r.cls(SERVER_ADDRESS);
        parseAddress = addr == null ? null : r.method(addr, "parseString", 1);
    }

    public static GameBridge create(Reflect r, Class<?> minecraftClass) {
        GameBridge b = new GameBridge(r, minecraftClass);
        return b.getInstance == null || b.player == null ? null : b;
    }

    private static Method staticAny(Method m) {
        return m != null && Modifier.isStatic(m.getModifiers()) ? m : null;
    }

    private static Method staticNoArg(Method m) {
        return m != null && Modifier.isStatic(m.getModifiers()) ? m : null;
    }

    public Reflect reflect() {
        return r;
    }

    public Object minecraft() {
        return Reflect.call(getInstance, null);
    }

    public Object player(Object mc) {
        return Reflect.get(player, mc);
    }

    public Object level(Object mc) {
        return level == null ? Boolean.TRUE : Reflect.get(level, mc);
    }

    public Object screen(Object mc) {
        if (screen != null) return Reflect.get(screen, mc);
        Object g = Reflect.get(gui, mc);
        if (g == null) return null;
        if (guiScreen == null) guiScreen = r.method(g.getClass(), "screen", 0);
        return Reflect.call(guiScreen, g);
    }

    public Object serverData(Object mc) {
        return Reflect.call(currentServer, mc);
    }

    public String serverIp(Object serverData) {
        Object ip = Reflect.get(serverIp, serverData);
        return ip instanceof String ? (String) ip : null;
    }

    public String userName(Object mc) {
        Object u = Reflect.call(getUser, mc);
        if (u == null) return null;
        if (userName == null) userName = r.method(u.getClass(), "getName", 0);
        Object n = Reflect.call(userName, u);
        return n instanceof String ? (String) n : null;
    }

    public Float health(Object p) {
        if (health == null) health = r.method(p.getClass(), "getHealth", 0);
        Object v = Reflect.call(health, p);
        return v instanceof Float ? (Float) v : null;
    }

    public Float maxHealth(Object p) {
        if (maxHealth == null) maxHealth = r.method(p.getClass(), "getMaxHealth", 0);
        Object v = Reflect.call(maxHealth, p);
        return v instanceof Float ? (Float) v : null;
    }

    public Integer food(Object p) {
        if (foodData == null) foodData = r.method(p.getClass(), "getFoodData", 0);
        Object fd = Reflect.call(foodData, p);
        if (fd == null) return null;
        if (foodLevel == null) foodLevel = r.method(fd.getClass(), "getFoodLevel", 0);
        Object v = Reflect.call(foodLevel, fd);
        return v instanceof Integer ? (Integer) v : null;
    }

    public ScreenKind kind(Object mc) {
        Object s = screen(mc);
        Object p = player(mc);
        if (s != null && deathCls != null && deathCls.isInstance(s)) return ScreenKind.DEATH;
        if (p != null && level(mc) != null) return ScreenKind.WORLD;
        if (s == null) return ScreenKind.LOADING;
        if (disconnectedCls != null && disconnectedCls.isInstance(s)) return ScreenKind.DISCONNECTED;
        if (connectCls != null && connectCls.isInstance(s)) return ScreenKind.CONNECTING;
        return ScreenKind.MENU;
    }

    public Object rawScreen(Object mc) {
        return screen(mc);
    }

    public String deathMessage(Object deathScreen) {
        Field f = r.field(deathScreen.getClass(), "causeOfDeath");
        return clean(r.text(Reflect.get(f, deathScreen)));
    }

    public String disconnectReason(Object disconnectedScreen) {
        Field reason = r.field(disconnectedScreen.getClass(), "reason");
        Object v = Reflect.get(reason, disconnectedScreen);
        if (v == null) {
            Field details = r.field(disconnectedScreen.getClass(), "details");
            Object d = Reflect.get(details, disconnectedScreen);
            if (d != null) v = Reflect.get(r.field(d.getClass(), "reason"), d);
        }
        return clean(r.text(v));
    }

    public Object disconnectParent(Object disconnectedScreen) {
        return Reflect.get(r.field(disconnectedScreen.getClass(), "parent"), disconnectedScreen);
    }

    public boolean canSend() {
        return getConnection != null || player != null;
    }

    public boolean canReconnect() {
        return startConnecting != null && parseAddress != null && serverDataCls != null;
    }

    public boolean send(Object mc, boolean command, String text) throws Exception {
        Object conn = Reflect.call(getConnection, mc);
        if (conn != null) {
            Method m = r.method(conn.getClass(), command ? "sendCommand" : "sendChat", 1);
            if (m != null && m.getParameterTypes()[0] == String.class) {
                m.invoke(conn, text);
                return true;
            }
        }
        Object p = player(mc);
        if (p == null) return false;
        Method signed = r.method(p.getClass(), command ? "commandSigned" : "chatSigned", 2);
        if (signed != null && signed.getParameterTypes()[0] == String.class) {
            signed.invoke(p, text, null);
            return true;
        }
        Method chat = r.method(p.getClass(), "chat", 1);
        if (chat != null && chat.getParameterTypes()[0] == String.class) {
            chat.invoke(p, command ? "/" + text : text);
            return true;
        }
        Method chat2 = r.method(p.getClass(), "chat", 2);
        if (chat2 != null && chat2.getParameterTypes()[0] == String.class) {
            chat2.invoke(p, command ? "/" + text : text, null);
            return true;
        }
        throw new NoSuchMethodException("no chat method");
    }

    public void reconnect(Object mc, Object serverData, Object parent) throws Exception {
        String ip = serverIp(serverData);
        if (ip == null) throw new IllegalStateException("no address");
        Object address = parseAddress.invoke(null, ip);
        Class<?>[] types = startConnecting.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int k = 0; k < types.length; k++) {
            Class<?> t = types[k];
            String moj = r.mojOf(t);
            if (t == boolean.class) args[k] = Boolean.FALSE;
            else if (t.isPrimitive()) throw new IllegalStateException("unexpected parameter " + t);
            else if (MINECRAFT.equals(moj)) args[k] = mc;
            else if (SERVER_ADDRESS.equals(moj)) args[k] = address;
            else if (SERVER_DATA.equals(moj)) args[k] = serverData;
            else if (SCREEN.equals(moj) || (parent != null && t.isInstance(parent) && k == 0)) args[k] = parent;
            else args[k] = null;
        }
        startConnecting.invoke(null, args);
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.replaceAll("§.", "").trim();
        if (t.isEmpty()) return null;
        return t.length() > 1024 ? t.substring(0, 1024) : t;
    }
}
