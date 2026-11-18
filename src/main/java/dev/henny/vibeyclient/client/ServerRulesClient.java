package dev.henny.vibeyclient.client;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.net.RulesPayloads;
import dev.henny.vibeyclient.net.RulesProtocol;

/**
 * Client side of the open server-rules channels (PLAY-LINKS.md §5, {@code docs/SERVER-RULES.md}): feeds
 * {@link ServerRules} from {@code vibey:rules}, shows one toast per server join, announces {@code vibey:hello} as soon
 * as the server registered that channel, and clears the rules when we leave. Client thread only.
 *
 * <p>Timing: Paper/Spigot servers announce their plugin channels ({@code minecraft:register}) shortly after the
 * login packet, and they only send to clients that registered {@code vibey:rules} (Fabric/NeoForge do that on join).
 * So the hello is retried every few ticks for the first {@link #HELLO_TICKS} ticks after joining instead of only once
 * at join time; servers of this jar negotiate channels earlier, then it goes out on the first tick.
 */
public final class ServerRulesClient {
    private ServerRulesClient() {}

    /** How long after joining we wait for the server to register {@code vibey:hello} (30 s). */
    static final int HELLO_TICKS = 600;

    private static boolean inWorld;
    private static int ticksInWorld;
    private static boolean helloSent;
    private static boolean toastShown;

    public static void init() {
        Lang.addFile("serverrules");
        // built-in keys of the core (the modules declare theirs in onLoad)
        ServerRules.supports("discord.server");
    }

    public static void onJoin() {
        inWorld = true;
        ticksInWorld = 0;
        helloSent = false;
        toastShown = false;
        ServerRules.clear();
    }

    public static void onLeave() {
        inWorld = false;
        helloSent = false;
        toastShown = false;
        ServerRules.clear();
    }

    public static void tick() {
        if (!inWorld || helloSent || !RulesPayloads.active()) return;
        ticksInWorld++;
        if (ticksInWorld > HELLO_TICKS || (ticksInWorld != 1 && ticksInWorld % 5 != 0)) return;
        ClientPlatform p = VibeyCore.clientPlatform();
        try {
            if (p == null || !p.canSendToServer(RulesPayloads.Hello.TYPE)) return;
            p.sendToServer(new RulesPayloads.Hello(helloJson()));
            helloSent = true;
            Vibey.LOG.info("Sent vibey:hello ({} supported keys)", ServerRules.supportedKeys().size());
        } catch (RuntimeException e) {
            helloSent = true; // do not retry a send that throws
            Vibey.LOG.debug("Could not send vibey:hello: {}", e.toString());
        }
    }

    static byte[] helloJson() {
        return RulesProtocol.encodeHello("vibey-client", BuildInfo.VERSION, ServerRules.supportedKeys(),
                BuildInfo.MINECRAFT, Vibey.platform().loader().toLowerCase(java.util.Locale.ROOT));
    }

    /** A {@code vibey:rules} packet arrived (client thread). */
    public static void handle(RulesPayloads.Rules payload) {
        if (!inWorld) return;
        RulesProtocol.Rules rules;
        try {
            rules = RulesProtocol.parseRules(payload.json());
        } catch (RulesProtocol.BadPacket e) {
            Vibey.LOG.warn("Ignoring bad vibey:rules from the server: {}", e.getMessage());
            return;
        }
        ServerRules.apply(rules.disable(), rules.message());
        Vibey.LOG.info("Server rules: disable {}{}", rules.disable(), rules.message() == null ? "" : " (\"" + rules.message() + "\")");
        if (toastShown || rules.isEmpty()) return;
        toastShown = true;
        String body = rules.message() != null ? rules.message() : Lang.tr("serverrules.toast.generic", rules.disable().size());
        Notify.show(new Notify.Toast(Lang.tr("serverrules.toast.title"), body, Notify.Kind.INFO, null, null));
    }
}
