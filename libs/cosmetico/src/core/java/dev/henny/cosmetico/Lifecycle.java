package dev.henny.cosmetico;

import dev.henny.cosmetico.host.BundleResources;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemListener;
import dev.henny.cosmetico.state.EmoteControl;

import java.util.function.Supplier;

final class Lifecycle implements ItemListener {
    private final Supplier<BundleResources> resources;
    private final EmoteControl emotes;

    Lifecycle(Supplier<BundleResources> resources, EmoteControl emotes) {
        this.resources = resources;
        this.emotes = emotes;
    }

    @Override
    public void replaced(Item old, Item now) {
        if (old != null && old.bundle() != null && old.bundle() != now.bundle()) resources.get().evict(old.bundle());
        if (now.bundle() != null && now.isEmote()) emotes.reload(now.id());
    }

    @Override
    public void removed(Item old) {
        if (old.bundle() != null) resources.get().evict(old.bundle());
    }
}
