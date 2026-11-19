package dev.henny.vibeyrules;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VibeyRules: tells Vibey Client which features to switch off on this server ({@code vibey:rules}) and logs which
 * players announced the client ({@code vibey:hello}). See docs/SERVER-RULES.md in vibey-client.
 *
 * <p>Timing: Bukkit only delivers plugin messages to players that registered the channel. Clients register their
 * channels ({@code minecraft:register}) right after joining, so the rules go out on {@link PlayerRegisterChannelEvent}
 * for {@code vibey:rules}; join (one tick later, for clients that registered during configuration), the client's
 * hello and world changes send again only if the rules for that player changed. No metrics, no external libraries.
 */
public final class VibeyRulesPlugin extends JavaPlugin implements Listener, PluginMessageListener {

    private static final class Peer {
        byte[] sent;
        Rules.Hello hello;
    }

    private final Map<UUID, Peer> peers = new ConcurrentHashMap<>();
    private volatile Rules.Config config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        load();
        getServer().getMessenger().registerOutgoingPluginChannel(this, Rules.RULES_CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, Rules.HELLO_CHANNEL, this);
        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand cmd = getCommand("vibeyrules");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
        for (Player p : Bukkit.getOnlinePlayers()) send(p); // /reload or a late plugin load
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        peers.clear();
    }

    private void load() {
        reloadConfig();
        FileConfiguration c = getConfig();
        Map<String, Map<String, Object>> worlds = new LinkedHashMap<>();
        ConfigurationSection ws = c.getConfigurationSection("worlds");
        if (ws != null) {
            for (String name : ws.getKeys(false)) {
                ConfigurationSection w = ws.getConfigurationSection(name);
                if (w != null) worlds.put(name, w.getValues(false));
            }
        }
        config = Rules.config(c.getBoolean("enabled", true), c.getList("disable", List.of()), c.getString("message", ""),
                worlds, c.getBoolean("log-hello", true));
        for (String p : config.problems()) getLogger().warning("config.yml: " + p);
        getLogger().info("Rules " + (config.enabled() ? "on: disable " + config.disable() : "off")
                + (config.worlds().isEmpty() ? "" : ", " + config.worlds().size() + " world override(s)"));
    }

    // ============================================================================================ events

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        peers.put(p.getUniqueId(), new Peer());
        Bukkit.getScheduler().runTask(this, () -> send(p));
    }

    @EventHandler
    public void onRegister(PlayerRegisterChannelEvent e) {
        if (Rules.RULES_CHANNEL.equals(e.getChannel())) onMain(() -> send(e.getPlayer()));
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent e) {
        send(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        peers.remove(e.getPlayer().getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!Rules.HELLO_CHANNEL.equals(channel)) return;
        Rules.Hello hello;
        try {
            hello = Rules.parseHello(message);
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Bad vibey:hello from " + player.getName() + ": " + ex.getMessage());
            return;
        }
        onMain(() -> {
            if (!player.isOnline()) return;
            Peer peer = peers.computeIfAbsent(player.getUniqueId(), k -> new Peer());
            boolean first = peer.hello == null;
            peer.hello = hello;
            Rules.Config c = config;
            if (first && c.logHello()) {
                List<String> missing = Rules.unsupported(c.rulesFor(player.getWorld().getName()).disable(), hello.supports());
                getLogger().info(player.getName() + " announced vibey:hello: " + hello.client() + " " + hello.version()
                        + ", supports " + hello.supports() + (missing.isEmpty() ? "" : " (does not know: " + missing + ")"));
            }
            send(player);
        });
    }

    private void onMain(Runnable r) {
        if (Bukkit.isPrimaryThread()) r.run();
        else Bukkit.getScheduler().runTask(this, r);
    }

    // ============================================================================================ sending

    /** Sends the player's rules if they changed since the last send and the client listens on vibey:rules. */
    private void send(Player player) {
        if (!player.isOnline()) return;
        Peer peer = peers.computeIfAbsent(player.getUniqueId(), k -> new Peer());
        Rules.Applied rules = player.hasPermission("vibeyrules.bypass") ? Rules.Applied.NONE : config.rulesFor(player.getWorld().getName());
        if (rules.isEmpty() && peer.sent == null) return; // nothing to say, nothing to clear
        byte[] json = Rules.encode(rules);
        if (peer.sent != null && Arrays.equals(peer.sent, json)) return;
        if (!player.getListeningPluginChannels().contains(Rules.RULES_CHANNEL)) return;
        player.sendPluginMessage(this, Rules.RULES_CHANNEL, json);
        peer.sent = json;
    }

    // ============================================================================================ command

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(java.util.Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                load();
                for (Player p : Bukkit.getOnlinePlayers()) send(p);
                sender.sendMessage("VibeyRules reloaded: " + (config.enabled() ? "disable " + config.disable() : "off")
                        + (config.problems().isEmpty() ? "" : " (" + config.problems().size() + " problem(s), see console)"));
            }
            case "status" -> {
                Rules.Config c = config;
                sender.sendMessage("VibeyRules " + getDescription().getVersion() + ": " + (c.enabled() ? "on" : "off")
                        + ", disable " + c.disable() + (c.message() == null ? "" : ", message \"" + c.message() + "\""));
                c.worlds().forEach((w, o) -> sender.sendMessage("  world " + w + ": disable " + (o.disable() == null ? c.disable() : o.disable())
                        + (o.message() == null ? "" : ", message \"" + o.message() + "\"")));
                int announced = 0, sent = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Peer peer = peers.get(p.getUniqueId());
                    if (peer == null) continue;
                    if (peer.sent != null) sent++;
                    if (peer.hello == null) continue;
                    announced++;
                    sender.sendMessage("  " + p.getName() + ": " + peer.hello.client() + " " + peer.hello.version() + ", "
                            + peer.hello.supports().size() + " supported key(s)" + (peer.sent != null ? ", rules sent" : "")
                            + (p.hasPermission("vibeyrules.bypass") ? ", bypass" : ""));
                }
                sender.sendMessage(announced + " player(s) announced Vibey Client, rules sent to " + sent);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        return List.of("reload", "status").stream().filter(s -> s.startsWith(args[0].toLowerCase(java.util.Locale.ROOT))).toList();
    }
}
