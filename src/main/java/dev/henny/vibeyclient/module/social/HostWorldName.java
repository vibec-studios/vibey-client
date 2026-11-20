package dev.henny.vibeyclient.module.social;

import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.backend.J;
import dev.henny.vibeyclient.module.social.model.Model.HostSession;
import dev.henny.vibeyclient.module.social.presence.WorldNamePatch;

final class HostWorldName {
    private final SocialModule module;
    private String sent;
    private String session;
    private long seenAt;

    HostWorldName(SocialModule module) {
        this.module = module;
    }

    void tick(String presenceWorld) {
        var backend = module.backend();
        HostSession mine = module.service().state.myHost;
        HostingService hs = Services.get(HostingService.class).orElse(null);
        if (backend == null || !backend.isReady() || mine == null || hs == null || !hs.isHosting()) return;
        long now = System.currentTimeMillis();
        if (!mine.id().equals(session)) {
            session = mine.id();
            seenAt = now;
        }
        String name = WorldNamePatch.name(SocialCompat.singleplayerWorld(), mine.worldName(), presenceWorld, now - seenAt);
        if (name == null) return;
        String key = mine.id() + "\n" + name;
        if (key.equals(sent)) return;
        sent = key;
        backend.request("PATCH", "/api/v1/hosting/sessions/" + BackendClient.enc(mine.id()), J.of("worldName", name))
                .exceptionally(t -> {
                    module.log().debug("world name not set: {}", Errors.text(t));
                    return null;
                });
    }
}
