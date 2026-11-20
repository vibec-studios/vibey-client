package dev.henny.companio.sync;

import dev.henny.companio.def.CompanionDef;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

public final class RemoteCompanions {
    private final Map<UUID, Puppet> puppets = new HashMap<>();
    private final BiFunction<String, String, CompanionDef> lookup;

    public RemoteCompanions(BiFunction<String, String, CompanionDef> lookupByKeyAndHash) {
        this.lookup = lookupByKeyAndHash;
    }

    public void receive(UUID player, String payload, double now) {
        if (SnapshotCodec.gone(payload)) {
            puppets.remove(player);
            return;
        }
        Snapshot s = SnapshotCodec.decode(payload);
        if (s == null) return;
        CompanionDef def = lookup.apply(s.key(), s.hash());
        if (def == null) {
            puppets.remove(player);
            return;
        }
        Puppet p = puppets.get(player);
        if (p == null || p.def() != def) puppets.put(player, new Puppet(def, s, now));
        else p.update(s, now);
    }

    public void remove(UUID player) {
        puppets.remove(player);
    }

    public void tick(double now, float dt) {
        puppets.values().removeIf(p -> p.stale(now));
        for (Puppet p : puppets.values()) p.tick(dt);
    }

    public Collection<Puppet> all() {
        return puppets.values();
    }

    public Map<UUID, Puppet> byPlayer() {
        return puppets;
    }

    public void clear() {
        puppets.clear();
    }
}
