package dev.henny.vibeyclient.dev;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.api.net.VibeyNet;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.model.Slot;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.player.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Dev-only remote control for multi-client tests (two real game clients + a server, see README "Live sync test"):
 * {@code -Dvibeyclient.devDriver=<port>} in a development environment opens a line protocol on 127.0.0.1:<port>.
 * One command per line, one answer line ({@code ok ...} / {@code err ...}); commands run on the client thread and use
 * the same services as the UI (Wardrobe, emotes, hosting, invites, toasts). Never active in release builds.
 * <pre>
 * status | players | avatar &lt;name|uuid&gt; | equip &lt;id&gt; | unequip &lt;slot&gt; | emote &lt;id&gt; | stopemote
 * wardrobe | close | connect &lt;host[:port]&gt; | disconnect | look &lt;name&gt; | approach &lt;name&gt; &lt;blocks&gt; | camera &lt;first|back|front&gt; | cmd &lt;command&gt;
 * online &lt;on|off&gt; | host | invite &lt;friend name&gt; | toastclick &lt;key prefix&gt; &lt;button&gt; | shot &lt;name&gt; | quit
 * rules | page &lt;module id&gt; | scroll &lt;px&gt; | respawn
 * </pre>
 */
public final class DevDriver {
    /**
     * Extra commands from code that only exists in some builds (e.g. {@code video ...} from the Fabric 1.21.11 video
     * screens): {@code handler(argument)} runs on the client thread and returns the answer line.
     */
    public static final java.util.Map<String, java.util.function.Function<String, String>> EXTRA = new java.util.concurrent.ConcurrentHashMap<>();

    private DevDriver() {}

    public static boolean active() {
        return Integer.getInteger("vibeyclient.devDriver") != null && Vibey.platform().isDevelopment();
    }

    public static void startIfRequested() {
        Integer port = Integer.getInteger("vibeyclient.devDriver");
        if (port == null || !Vibey.platform().isDevelopment()) return;
        Thread t = new Thread(() -> serve(port), "vibeyclient-devdriver");
        t.setDaemon(true);
        t.start();
    }

    private static void serve(int port) {
        try (ServerSocket server = new ServerSocket(port, 4, InetAddress.getLoopbackAddress())) {
            Vibey.LOG.info("[vibeyclient-devdriver] listening on 127.0.0.1:{}", port);
            while (true) {
                Socket s = server.accept();
                Thread c = new Thread(() -> session(s), "vibeyclient-devdriver-conn");
                c.setDaemon(true);
                c.start();
            }
        } catch (Exception e) {
            Vibey.LOG.error("[vibeyclient-devdriver] stopped", e);
        }
    }

    private static void session(Socket s) {
        try (s; BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                String cmd = line.trim();
                if (cmd.isEmpty()) continue;
                CompletableFuture<String> f = new CompletableFuture<>();
                Tasks.onMain(() -> {
                    try {
                        f.complete(exec(cmd));
                    } catch (Throwable t) {
                        f.complete("err " + t);
                    }
                });
                String answer;
                try {
                    answer = f.get(15, TimeUnit.SECONDS);
                } catch (Exception e) {
                    answer = "err timeout";
                }
                out.println(answer.replace('\n', ' '));
            }
        } catch (Exception ignored) {
        }
    }

    private static String exec(String line) {
        Minecraft mc = Minecraft.getInstance();
        String[] a = line.split(" ", 2);
        String cmd = a[0].toLowerCase(Locale.ROOT);
        String arg = a.length > 1 ? a[1].trim() : "";
        Wardrobe wd = Services.get(Wardrobe.class).orElse(null);
        switch (cmd) {
            case "status": {
                VibeBackend b = Services.get(VibeBackend.class).orElse(null);
                return "ok screen=" + (ClientCompat.screen() == null ? "none" : ClientCompat.screen().getClass().getSimpleName())
                        + " world=" + (mc.level != null) + " name=" + ClientCompat.playerName()
                        + " backend=" + (b == null ? "none" : b.isReady() ? "ready" : "connecting")
                        + " online=" + GlobalSettings.online() + " serverChannel=" + VibeyNet.available()
                        + " loadout=" + loadout(wd);
            }
            case "players": {
                if (mc.level == null) return "err no world";
                List<String> l = new ArrayList<>();
                for (Player p : mc.level.players()) l.add(p.getName().getString() + "=" + p.getUUID());
                return "ok " + String.join(",", l);
            }
            case "avatar": {
                UUID id = resolve(mc, arg);
                if (id == null) return "err unknown player " + arg;
                Avatar st = Cosmetico.engine().avatars.peek(id);
                if (st == null) return "ok " + id + " none";
                StringBuilder sb = new StringBuilder("ok ").append(id);
                for (Slot sl : Slot.all()) if (st.loadout.base[sl.ordinal()] != null) sb.append(' ').append(sl.id()).append('=').append(st.loadout.base[sl.ordinal()]);
                sb.append(" emote=").append(st.emote == null ? "none" : st.emote.id + (st.emote.stopping() ? "(stopping)" : ""));
                return sb.toString();
            }
            case "equip": {
                if (wd == null) return "err no wardrobe";
                Item item = Cosmetico.items().get(arg);
                if (item == null) return "err unknown item " + arg;
                if (!wd.canUse(item)) return "err locked " + arg;
                wd.equip(item);
                return "ok " + loadout(wd);
            }
            case "unequip": {
                if (wd == null) return "err no wardrobe";
                Slot sl = Slot.of(arg);
                if (sl == null) return "err unknown slot " + arg;
                wd.unequip(sl);
                return "ok " + loadout(wd);
            }
            case "emote":
                if (wd == null) return "err no wardrobe";
                wd.playEmote(arg);
                return "ok playing=" + wd.playingEmote();
            case "stopemote":
                if (wd == null) return "err no wardrobe";
                wd.stopEmote();
                return "ok";
            case "wardrobe": // wardrobe [slot]
                dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi.openWardrobe(ClientCompat.screen(),
                        arg == null || arg.isBlank() ? null : dev.henny.cosmetico.model.Slot.of(arg.trim()));
                return "ok";
            case "close":
                ClientCompat.openScreen(null);
                return "ok";
            case "connect": {
                String host = arg;
                int port = 25565;
                int c = arg.lastIndexOf(':');
                if (c > 0) {
                    host = arg.substring(0, c);
                    port = Integer.parseInt(arg.substring(c + 1));
                }
                dev.henny.vibeyclient.module.social.SocialCompat.connect(arg, host, port);
                return "ok";
            }
            case "disconnect":
                if (mc.level == null) return "err no world";
                dev.henny.vibeyclient.module.social.SocialCompat.leaveWorld();
                return "ok";
            case "look": {
                if (mc.player == null || mc.level == null) return "err no world";
                UUID id = resolve(mc, arg);
                Player p = id == null ? null : mc.level.getPlayerByUUID(id);
                if (p == null) return "err not here " + arg;
                double dx = p.getX() - mc.player.getX(), dy = p.getEyeY() - 0.4 - mc.player.getEyeY(), dz = p.getZ() - mc.player.getZ();
                float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
                mc.player.setYRot(yaw);
                mc.player.setXRot(pitch);
                mc.player.setYHeadRot(yaw);
                return "ok yaw=" + yaw + " pitch=" + pitch + " dist=" + Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
            case "approach": { // approach <name> <blocks>: walk-teleport to that distance in front of a player
                if (mc.player == null || mc.level == null) return "err no world";
                String[] p = arg.split(" ");
                UUID id = resolve(mc, p[0]);
                Player other = id == null ? null : mc.level.getPlayerByUUID(id);
                if (other == null) return "err not here " + p[0];
                double want = p.length > 1 ? Double.parseDouble(p[1]) : 3;
                double dx = mc.player.getX() - other.getX(), dz = mc.player.getZ() - other.getZ();
                double len = Math.max(1e-3, Math.sqrt(dx * dx + dz * dz));
                mc.player.setPos(other.getX() + dx / len * want, other.getY(), other.getZ() + dz / len * want);
                return "ok";
            }
            case "camera":
                dev.henny.cosmetico.mc.compat.McCamera.set(switch (arg) {
                    case "back" -> net.minecraft.client.CameraType.THIRD_PERSON_BACK;
                    case "front" -> net.minecraft.client.CameraType.THIRD_PERSON_FRONT;
                    default -> net.minecraft.client.CameraType.FIRST_PERSON;
                });
                return "ok";
            case "cmd":
                if (mc.player == null) return "err no world";
                mc.player.connection.sendCommand(arg.startsWith("/") ? arg.substring(1) : arg);
                return "ok";
            case "online":
                GlobalSettings.onlineFeatures.set(!"off".equals(arg));
                return "ok online=" + GlobalSettings.online();
            case "host": {
                HostingService hs = Services.get(HostingService.class).orElse(null);
                if (hs == null) return "err no hosting service";
                if (arg.equals("status")) return "ok canHost=" + hs.canHost() + " hosting=" + hs.isHosting() + " address=" + hs.address() + ":" + hs.port();
                if (!hs.canHost()) return "err cannot host";
                hs.startHosting();
                return "ok starting";
            }
            case "invite": {
                var social = ModuleManager.get().get(dev.henny.vibeyclient.module.social.SocialModule.class);
                if (social == null || !social.isEnabled()) return "err no social";
                for (var f : social.service().state.friends.values()) {
                    if (f.user().name().equalsIgnoreCase(arg)) {
                        social.invites().inviteToMyWorld(List.of(f.user().id()));
                        return "ok invited " + f.user().id() + " availability=" + social.invites().availability();
                    }
                }
                return "err not a friend " + arg;
            }
            case "toastclick": {
                String[] p = arg.split(" ");
                int[] at = dev.henny.vibeyclient.ui.toast.ToastStack.buttonCenter(p[0], p.length > 1 ? Integer.parseInt(p[1]) : 0);
                if (at == null) return "err no such toast " + p[0];
                return dev.henny.vibeyclient.ui.toast.ToastStack.click(at[0], at[1], 0) ? "ok clicked " + at[0] + "," + at[1] : "err click missed";
            }
            case "toasts":
                return "ok " + dev.henny.vibeyclient.ui.toast.ToastStack.debugKeys();
            case "rules": // server rules (PLAY-LINKS.md §5): what the server disabled, what we announced
                return "ok active=" + dev.henny.vibeyclient.api.ServerRules.active()
                        + " message=" + dev.henny.vibeyclient.api.ServerRules.message()
                        + " supported=" + dev.henny.vibeyclient.api.ServerRules.supportedKeys();
            case "respawn":
                if (mc.player == null) return "err no player";
                mc.player.respawn();
                ClientCompat.openScreen(null);
                return "ok";
            case "scroll": { // scroll <px>: scrolls the open module page (the wheel only works under the real cursor)
                dev.henny.vibeyclient.ui.screen.ModulesPanel.scrollPageTo(Integer.parseInt(arg));
                return "ok";
            }
            case "page": { // page <module id>: open the Vibey menu on that module's settings page
                ClientCompat.openScreen(null);
                dev.henny.vibeyclient.ui.vui.Menus.openModule(null, arg.isEmpty() ? null : arg);
                return "ok " + arg;
            }
            case "shot":
                shot(mc);
                return "ok";
            case "quit":
                mc.stop();
                return "ok";
            default: {
                java.util.function.Function<String, String> extra = EXTRA.get(cmd);
                return extra != null ? extra.apply(arg) : "err unknown command " + cmd;
            }
        }
    }

    private static UUID resolve(Minecraft mc, String nameOrId) {
        try {
            return UUID.fromString(nameOrId);
        } catch (IllegalArgumentException ignored) {
        }
        if (mc.level == null) return null;
        for (Player p : mc.level.players()) if (p.getName().getString().equalsIgnoreCase(nameOrId)) return p.getUUID();
        return null;
    }

    private static String loadout(Wardrobe wd) {
        if (wd == null) return "-";
        StringBuilder sb = new StringBuilder("{");
        for (Slot sl : Slot.all()) {
            String id = wd.equipped(sl);
            if (id != null) sb.append(sb.length() > 1 ? "," : "").append(sl.id()).append('=').append(id);
        }
        return sb.append('}').toString();
    }

    private static void shot(Minecraft mc) {
        //? if >=26.2 {
        /*Screenshot.grab(mc.gameDirectory, mc.gameRenderer.mainRenderTarget(), msg -> Vibey.LOG.info("[vibeyclient-devdriver] {}", msg.getString()));
        *///?} else
        Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), msg -> Vibey.LOG.info("[vibeyclient-devdriver] {}", msg.getString()));
    }
}
