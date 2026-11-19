package dev.henny.cosmetico.dev;

import com.google.gson.JsonObject;
import dev.henny.cosmetico.host.BundleResources;
import dev.henny.cosmetico.host.LocalActions;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.parse.BundleException;
import dev.henny.cosmetico.parse.Bundles;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemRegistry;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.cosmetico.thumbs.DevThumbnails;
import dev.henny.cosmetico.version.VersionGate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class DevItemService implements DevItems {
    private final ItemRegistry items;
    private final DevThumbnails thumbnails;
    private final Supplier<LocalActions> actions;
    private final Supplier<BundleResources> resources;
    private final Supplier<VersionGate> gate;

    public DevItemService(ItemRegistry items, DevThumbnails thumbnails, Supplier<LocalActions> actions,
                          Supplier<BundleResources> resources, Supplier<VersionGate> gate) {
        this.items = items;
        this.thumbnails = thumbnails;
        this.actions = actions;
        this.resources = resources;
        this.gate = gate;
    }

    @Override
    public PushResult push(byte[] bytes, PushAction action, byte[] thumbnail) {
        try {
            return register(Bundles.parse(bytes, gate.get()), action, thumbnail);
        } catch (BundleException e) {
            return new PushResult(null, false, List.of(), e.getMessage());
        }
    }

    @Override
    public PushResult push(JsonObject json, PushAction action, byte[] thumbnail) {
        String id = json.has("id") && json.get("id").isJsonPrimitive() ? json.get("id").getAsString() : null;
        try {
            return register(Bundles.parse(json, gate.get()), action, thumbnail);
        } catch (BundleException e) {
            return new PushResult(id, false, List.of(), e.getMessage());
        } catch (RuntimeException e) {
            return new PushResult(id, false, List.of(), "Internal error: " + e);
        }
    }

    private PushResult register(Bundle b, PushAction action, byte[] thumbnail) {
        List<String> warnings = b.warnings;
        String thumb = null;
        if (thumbnail != null) {
            String bad = ThumbnailCheck.check(thumbnail);
            if (bad == null) thumb = thumbnails.put(b.id, thumbnail);
            else {
                warnings = new ArrayList<>(warnings);
                warnings.add("thumbnail ignored: " + bad);
            }
        }
        if (thumb == null) thumb = thumbnails.key(b.id);
        resources.get().prepare(b);
        Item item = Item.of(b, ItemSource.DEV, -1, thumb, null, items.nextRevision());
        items.put(item);
        if (b.isEmote()) {
            if (action == PushAction.PLAY || action == PushAction.EQUIP) actions.get().playEmote(b.id);
        } else if (action == PushAction.EQUIP) {
            actions.get().equip(item);
        }
        return new PushResult(b.id, true, warnings, null);
    }

    @Override
    public boolean remove(String id) {
        Item old = items.remove(id, ItemSource.DEV);
        if (old == null) return false;
        thumbnails.remove(id);
        if (id.equals(actions.get().playingEmote()) && items.get(id) == null) actions.get().stopEmote();
        return true;
    }

    @Override
    public List<String> ids() {
        List<String> out = new ArrayList<>();
        for (Item i : items.all()) if (i.isDev()) out.add(i.id());
        return out;
    }
}
