package dev.henny.vibeyclient.module.social;

import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.backend.J;
import dev.henny.vibeyclient.module.social.model.Model.HostSession;

final class HostWorldName {
    private final SocialModule module;
    private String sent;

    HostWorldName(SocialModule module) {
        this.module = module;
    }

    void tick() {
        var backend = module.backend();
        HostSession mine = module.service().state.myHost;
        HostingService hs = Services.get(HostingService.class).orElse(null);
        if (backend == null || !backend.isReady() || mine == null || hs == null || !hs.isHosting()) return;
        String name = name(SocialCompat.singleplayerWorld());
        if (name == null || name.equals(mine.worldName())) return;
        String key = mine.id() + "\n" + name;
        if (key.equals(sent)) return;
        sent = key;
        backend.request("PATCH", "/api/v1/hosting/sessions/" + BackendClient.enc(mine.id()), J.of("worldName", name))
                .exceptionally(t -> {
                    module.log().debug("world name not set: {}", Errors.text(t));
                    return null;
                });
    }

    static String name(String world) {
        if (world == null || world.isBlank()) return null;
        String t = world.trim();
        return t.length() <= 100 ? t : t.substring(0, 100).trim();
    }
}
