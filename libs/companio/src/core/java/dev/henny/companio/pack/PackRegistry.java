package dev.henny.companio.pack;

import dev.henny.companio.def.CompanionDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PackRegistry {
    private volatile List<Pack> packs = List.of();
    private final Map<String, CompanionDef> lastGood = new HashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public synchronized void replace(List<Pack> loaded) {
        Map<String, Pack> byId = new LinkedHashMap<>();
        List<Pack> out = new ArrayList<>();
        for (Pack p : loaded) {
            if (p.id() != null && byId.containsKey(p.id())) out.remove(byId.get(p.id()));
            if (p.id() != null) byId.put(p.id(), p);
            out.add(p);
        }
        Map<String, CompanionDef> good = new HashMap<>();
        for (Pack p : out) {
            if (p.id() == null) continue;
            for (CompanionDef c : p.companions()) good.put(c.key(), c);
            if (p.errors() > 0) for (Map.Entry<String, CompanionDef> e : lastGood.entrySet()) {
                if (e.getValue().packId().equals(p.id()) && !good.containsKey(e.getKey())) good.put(e.getKey(), e.getValue());
            }
        }
        lastGood.clear();
        lastGood.putAll(good);
        packs = List.copyOf(out);
        for (Runnable r : listeners) r.run();
    }

    public List<Pack> all() {
        return packs;
    }

    public Pack pack(String id) {
        for (Pack p : packs) if (id.equals(p.id())) return p;
        return null;
    }

    public Pack byLocation(String location) {
        for (Pack p : packs) if (location.equals(p.location())) return p;
        return null;
    }

    public List<Pack> active(List<String> activeIds) {
        List<Pack> out = new ArrayList<>();
        for (String id : activeIds) {
            Pack p = pack(id);
            if (p != null) out.add(p);
        }
        return out;
    }

    public synchronized CompanionDef companion(String key, List<String> activeIds) {
        int i = key.indexOf(':');
        if (i < 0 || !activeIds.contains(key.substring(0, i))) return null;
        return lastGood.get(key);
    }

    public synchronized List<CompanionDef> companions(List<String> activeIds) {
        List<CompanionDef> out = new ArrayList<>();
        for (String id : activeIds) {
            for (CompanionDef c : lastGood.values()) if (c.packId().equals(id)) out.add(c);
        }
        out.sort((a, b) -> activeIds.indexOf(a.packId()) != activeIds.indexOf(b.packId())
                ? Integer.compare(activeIds.indexOf(a.packId()), activeIds.indexOf(b.packId())) : a.id().compareTo(b.id()));
        return out;
    }

    public synchronized CompanionDef matching(String key, String hash) {
        CompanionDef c = lastGood.get(key);
        if (c == null) return null;
        Pack p = pack(c.packId());
        return p != null && hash.equals(p.hash()) ? c : null;
    }

    public void listen(Runnable r) {
        listeners.add(r);
    }
}
