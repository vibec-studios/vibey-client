package dev.henny.vibeyclient.module.multiplayer.host;

import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.module.multiplayer.MpCompat;
import dev.henny.vibeyclient.module.multiplayer.MpLoader;
import dev.henny.vibeyclient.module.multiplayer.MultiplayerModule;
import dev.henny.vibeyclient.module.multiplayer.T;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;

/**
 * The one hosting session of this client (ported from Vibey Multiplayer). Started and stopped from the host screen (client thread); the tunnel
 * reports back from its own thread, so everything the screen reads is volatile.
 */
public final class HostSession {
    public enum Status { OFF, CONNECTING, ONLINE, RECONNECTING, ERROR }

    private static volatile Status status = Status.OFF;
    private static volatile String address = "";
    private static volatile String error = "";
    private static volatile String notice = "";
    private static volatile long onlineSince;
    private static Tunnel tunnel;
    private static IntegratedServer hosted;
    private static IntegratedServer openedFor;
    private static int lanPort = -1;

    private HostSession() {}

    public static Status status() {
        return status;
    }

    /** The public address, or the last known one while offline. */
    public static String address() {
        String a = address;
        return a.isEmpty() ? HostConfig.get().lastAddress : a;
    }

    public static String error() {
        return error;
    }

    /** Non-fatal hint from the relay (e.g. "too many new addresses"). */
    public static String notice() {
        return notice;
    }

    public static long onlineSince() {
        return onlineSince;
    }

    public static int lanPort() {
        return lanPort;
    }

    public static boolean isHosting() {
        Status s = status;
        return s != Status.OFF && s != Status.ERROR && hosted != null
                && hosted == Minecraft.getInstance().getSingleplayerServer();
    }

    static boolean openedByUs(MinecraftServer server) {
        return server == openedFor;
    }

    /** Players in the hosted world (host included), -1 when not hosting. */
    public static int playerCount() {
        IntegratedServer s = hosted;
        return s == null || !isHosting() ? -1 : s.getPlayerCount();
    }

    public static boolean canHost() {
        return Minecraft.getInstance().getSingleplayerServer() != null;
    }

    // -------------------------------------------------------------------------------------------- start / stop

    /** Opens the world and goes online. Call on the client thread. */
    public static void start() {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || isHosting()) return;
        HostConfig cfg = HostConfig.get();
        error = "";
        notice = "";

        if (!server.isPublished()) {
            int port = cfg.lanPort > 0 ? cfg.lanPort : HttpUtil.getAvailablePort();
            GameType mode = GameType.byName(cfg.gameMode, GameType.SURVIVAL);
            if (!MpCompat.publish(server, mode, cfg.cheats, port)) {
                fail(T.get("error.port", port));
                return;
            }
            openedFor = server;
        } else {
            server.execute(() -> MpCompat.setCheats(server, cfg.cheats));
        }
        lanPort = server.getPort();
        hosted = server;
        boolean pvp = cfg.pvp;
        server.execute(() -> MpCompat.setPvp(server, pvp));

        User user = mc.getUser();
        String mcVersion = MpLoader.minecraftVersion();
        status = Status.CONNECTING;
        Tunnel t = new Tunnel(cfg.relay, lanPort, user.getName(), user.getProfileId(), mcVersion,
                MpCompat::joinServer, Access::relayMessage, new Tunnel.Listener() {
                    @Override
                    public void onState(Tunnel.State state, String address, String error) {
                        onTunnel(state, address, error);
                    }

                    @Override
                    public void onPlayer(String name, String ip) {
                        Access.onRelayJoin(name, ip);
                    }

                    @Override
                    public void onBlocked(String name, String ip, String reason) {
                        Access.onRelayBlocked(name, ip, reason);
                    }
                });
        tunnel = t;
        t.start();
        MultiplayerModule.LOG.info("Hosting world on LAN port {} through {}", lanPort, cfg.relay);
    }

    /** Goes offline and sends guests home. */
    public static void stop() {
        Tunnel t = tunnel;
        tunnel = null;
        if (t != null) t.close();
        IntegratedServer server = hosted;
        hosted = null;
        boolean wasHosting = status != Status.OFF && status != Status.ERROR;
        status = Status.OFF;
        address = "";
        onlineSince = 0;
        if (wasHosting) changed(false, null);
        PlayLinks.onStopped();
        if (server != null) {
            server.execute(() -> {
                Guests.kickAllGuests(server, "kick.stopped");
                if (MpCompat.unpublish(server)) openedFor = null;
            });
        }
    }

    /** Tells the relay about a changed whitelist / ban list right away. */
    static void pushAccess() {
        Tunnel t = tunnel;
        if (t != null) t.pushAccess();
    }

    public static void rotate() {
        Tunnel t = tunnel;
        if (t != null && status == Status.ONLINE) t.rotate();
    }

    public static void onServerStopping(MinecraftServer server) {
        if (server == hosted) stop();
        if (server == openedFor) openedFor = null;
        Guests.onServerStopped();
    }

    // -------------------------------------------------------------------------------------------- live settings

    public static void applyCheats() {
        boolean cheats = HostConfig.get().cheats;
        if (hosted != null) Guests.run(server -> MpCompat.setCheats(server, cheats));
    }

    public static void applyPvp() {
        boolean pvp = HostConfig.get().pvp;
        if (hosted != null) Guests.run(server -> MpCompat.setPvp(server, pvp));
    }

    public static void applyGameMode() {
        if (hosted != null) Guests.applyGameMode(HostConfig.get().gameMode);
    }

    // -------------------------------------------------------------------------------------------- tunnel callbacks

    private static void onTunnel(Tunnel.State state, String addr, String err) {
        switch (state) {
            case CONNECTING -> {
                if (status != Status.ONLINE) status = address.isEmpty() ? Status.CONNECTING : Status.RECONNECTING;
            }
            case ONLINE -> {
                if (addr != null) {
                    boolean changed = !addr.equals(address);
                    address = addr;
                    if (!addr.equals(HostConfig.get().lastAddress)) {
                        HostConfig.get().lastAddress = addr;
                        Minecraft.getInstance().execute(HostConfig::save);
                    }
                    boolean wasOnline = status == Status.ONLINE;
                    if (changed || onlineSince == 0) onlineSince = System.currentTimeMillis();
                    status = Status.ONLINE;
                    error = "";
                    if (changed || !wasOnline) {
                        changed(true, addr);
                        Minecraft.getInstance().execute(() -> {
                            if (isHosting()) PlayLinks.onOnline(addr);
                        });
                        if (changed) MultiplayerModule.toast(T.get("toast.online"), T.get("toast.online.body", addr), true);
                    }
                }
                if (err != null) notice = err;
            }
            case RETRYING -> {
                if (tunnel != null) {
                    status = Status.RECONNECTING;
                    error = err == null ? "" : err;
                }
            }
            case FAILED -> {
                String text = "replaced".equals(err) ? T.get("error.replaced") : (err == null ? "" : err);
                fail(text);
            }
            case CLOSED -> {
            }
        }
    }

    private static void fail(String text) {
        Tunnel t = tunnel;
        tunnel = null;
        if (t != null) t.close();
        hosted = null;
        address = "";
        status = Status.ERROR;
        error = text;
        changed(false, null);
        PlayLinks.onStopped();
        MultiplayerModule.toast(T.get("toast.failed"), text, false);
    }

    /** Posts {@link HostingService.HostingChanged} on the client thread. */
    private static void changed(boolean hosting, String addr) {
        String host = hostPart(addr);
        Minecraft.getInstance().execute(() -> Events.post(new HostingService.HostingChanged(hosting, host)));
    }

    /** "a-b-12.axolite.cc:26400" -> "a-b-12.axolite.cc" (null stays null). */
    public static String hostPart(String addr) {
        if (addr == null) return null;
        int colon = addr.lastIndexOf(':');
        return colon > 0 && addr.indexOf(']') < colon ? addr.substring(0, colon) : addr;
    }

    /** Port of the public address, 25565 if it has none. */
    public static int portPart(String addr) {
        int colon = addr == null ? -1 : addr.lastIndexOf(':');
        if (colon > 0 && addr.indexOf(']') < colon) {
            try {
                return Integer.parseInt(addr.substring(colon + 1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 25565;
    }
}
