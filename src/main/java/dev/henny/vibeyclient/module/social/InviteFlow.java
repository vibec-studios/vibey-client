package dev.henny.vibeyclient.module.social;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.backend.J;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.social.model.Model.HostSession;
import dev.henny.vibeyclient.module.social.ui.ConfirmScreen;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Inviting friends into the world we host (starting hosting through the multiplayer module's
 * {@link HostingService} first if needed) and joining a friend's world (leaving the current one after a
 * confirmation, then the vanilla connect flow).
 */
public final class InviteFlow {
    private final SocialModule module;
    private List<String> pendingInvite;
    private long pendingSince;

    InviteFlow(SocialModule module) {
        this.module = module;
        Events.subscribe(HostingService.HostingChanged.class, e -> {
            if (e.hosting() && pendingInvite != null && System.currentTimeMillis() - pendingSince < 90_000) {
                List<String> ids = pendingInvite;
                pendingInvite = null;
                sendWhenSessionKnown(ids, 0);
            }
            module.service().refreshHosts();
        });
    }

    public enum Availability { OK, START_HOSTING, NO_WORLD, NO_MODULE }

    /** Can we invite into the current world right now? */
    public Availability availability() {
        HostingService hs = Services.get(HostingService.class).orElse(null);
        if (hs == null) return Availability.NO_MODULE;
        if (hs.isHosting()) return Availability.OK;
        if (hs.canHost()) return Availability.START_HOSTING;
        return Availability.NO_WORLD;
    }

    /** Invites {@code userIds} into our world: starts hosting first (after asking) when needed. */
    public void inviteToMyWorld(List<String> userIds) {
        HostingService hs = Services.get(HostingService.class).orElse(null);
        switch (availability()) {
            case NO_MODULE -> Notify.warn(module.tr("invite.noModule"), module.tr("invite.noModuleHint"));
            case NO_WORLD -> Notify.warn(module.tr("invite.noWorld"), module.tr("invite.noWorldHint"));
            case START_HOSTING -> ConfirmScreen.ask(module.tr("invite.startHostingTitle"), module.tr("invite.startHostingText"),
                    module.tr("invite.startHosting"), () -> {
                        pendingInvite = List.copyOf(userIds);
                        pendingSince = System.currentTimeMillis();
                        Notify.info(module.tr("invite.startingHost"), null);
                        hs.startHosting();
                    });
            case OK -> sendWhenSessionKnown(userIds, 0);
        }
    }

    /** The relay reports the host to the backend a moment after it went online: look the session up, retry a bit. */
    private void sendWhenSessionKnown(List<String> userIds, int attempt) {
        HostingService hs = Services.get(HostingService.class).orElse(null);
        if (hs == null || !hs.isHosting()) return;
        String address = hs.address();
        module.backend().request("GET", "/api/v1/hosting/sessions", null).whenCompleteAsync((e, t) -> {
            String sessionId = null;
            if (t == null) {
                for (HostSession s : J.list(e, HostSession::from)) {
                    if (s.online() && (address == null || Objects.equals(s.address(), address))) sessionId = s.id();
                }
            }
            if (sessionId == null && attempt < 4 && (t == null || !"feature_disabled".equals(BackendClient.unwrap(t).code))) {
                Tasks.later(1_500, () -> Tasks.onMain(() -> sendWhenSessionKnown(userIds, attempt + 1)));
                return;
            }
            send(userIds, sessionId, address, hs.port());
        }, SocialService.MAIN);
    }

    private void send(List<String> userIds, String hostSessionId, String address, int port) {
        String world = SocialCompat.singleplayerWorld();
        JsonObject target = J.of("name", world == null ? "Minecraft" : world, "mcVersion", BuildInfo.MINECRAFT,
                "loader", Vibey.platform().loader().toLowerCase(Locale.ROOT));
        JsonObject body = J.of("kind", "world", "toUserIds", J.strings(userIds), "target", target);
        if (hostSessionId != null) {
            body.addProperty("hostSessionId", hostSessionId);
        } else if (address != null) {
            // relay session not (yet) known to the backend: send the address itself
            body.add("connection", J.of("type", "relay", "address", address, "port", port <= 0 ? 25565 : port));
        } else {
            Notify.error(module.tr("invite.failed"), null);
            return;
        }
        module.service().sendInvite(body).thenRun(() -> Tasks.onMain(() -> Notify.success(
                module.tr(userIds.size() == 1 ? "invite.sentOne" : "invite.sentMany",
                        userIds.size() == 1 ? module.service().state.nameOf(userIds.get(0)) : userIds.size()), null)));
    }

    /** Joins {@code address:port}, leaving the open world/server first (with a confirmation). Client thread. */
    public void join(String name, String address, int port) {
        if (address == null) return;
        Runnable go = () -> {
            if (SocialCompat.inWorld()) SocialCompat.leaveWorld();
            SocialCompat.connect(name, address, port);
        };
        if (SocialCompat.inWorld()) {
            String what = SocialCompat.singleplayerWorld() != null ? module.tr("join.leaveWorld") : module.tr("join.leaveServer");
            ConfirmScreen.ask(module.tr("join.title", name == null ? address : name), what, module.tr("action.join"), go);
        } else {
            ClientCompat.openScreen(null);
            go.run();
        }
    }
}
