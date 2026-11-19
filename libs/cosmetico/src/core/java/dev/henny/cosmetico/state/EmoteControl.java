package dev.henny.cosmetico.state;

import dev.henny.cosmetico.anim.Clock;
import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.host.BundleResources;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemRegistry;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class EmoteControl implements Emotes {
    private final AvatarRegistry avatars;
    private final ItemRegistry items;
    private final Clock clock;
    private final Supplier<BundleResources> resources;
    private final List<EmoteListener> listeners = new CopyOnWriteArrayList<>();

    public EmoteControl(AvatarRegistry avatars, ItemRegistry items, Clock clock, Supplier<BundleResources> resources) {
        this.avatars = avatars;
        this.items = items;
        this.clock = clock;
        this.resources = resources;
    }

    @Override
    public boolean play(UUID player, String emoteId) {
        return play(avatars.get(player), emoteId);
    }

    public boolean play(Avatar avatar, String emoteId) {
        Item item = items.get(emoteId);
        Bundle b = item == null ? null : item.bundle();
        if (b == null || !b.isEmote() || b.emoteAnimation() == null) return false;
        double now = clock.now();
        resources.get().prefetchSounds(b);
        avatar.emote = new EmotePlay(emoteId, b, now);
        avatar.emote.update(now);
        for (EmoteListener l : listeners) l.started(avatar.uuid, emoteId);
        return true;
    }

    @Override
    public void stop(UUID player) {
        Avatar a = avatars.peek(player);
        if (a != null) stop(a);
    }

    public void stop(Avatar avatar) {
        if (avatar.emote == null) return;
        avatar.emote.stop(clock.now());
        for (EmoteListener l : listeners) l.stopped(avatar.uuid, avatar.emote.id);
    }

    @Override
    public String playing(UUID player) {
        Avatar a = avatars.peek(player);
        return a == null || a.emote == null || a.emote.stopping() ? null : a.emote.id;
    }

    @Override
    public AutoCloseable listen(EmoteListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void reload(String id) {
        for (Avatar a : avatars.all()) if (a.emote != null && a.emote.id.equals(id) && !a.emote.stopping()) play(a, id);
    }

    public void tick(Avatar a, double now) {
        EmotePlay e = a.emote;
        if (e == null) return;
        e.update(now);
        if (!e.finished(now)) return;
        a.emote = null;
        for (EmoteListener l : listeners) l.ended(a.uuid, e.id);
    }
}
