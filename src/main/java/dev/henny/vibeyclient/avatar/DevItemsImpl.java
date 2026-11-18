package dev.henny.vibeyclient.avatar;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.bundle.BundleException;
import dev.henny.vibeyclient.avatar.bundle.BundleParser;
import dev.henny.vibeyclient.avatar.render.GpuBundles;

import java.util.ArrayList;
import java.util.List;

/**
 * Dev items (devlink pushes): parsed on the calling thread, registered with source DEV (wins over builtin/catalog
 * items with the same id), equipped or played on the local player. Never sent anywhere.
 */
public final class DevItemsImpl implements DevItems {
    private final Wardrobe wardrobe;

    public DevItemsImpl(Wardrobe wardrobe) {
        this.wardrobe = wardrobe;
    }

    @Override
    public PushResult push(byte[] bytes, Action action) {
        return push(bytes, action, null);
    }

    @Override
    public PushResult push(byte[] bytes, Action action, byte[] thumbnail) {
        try {
            return register(BundleParser.parse(bytes), action, thumbnail);
        } catch (BundleException e) {
            return new PushResult(null, false, List.of(), e.getMessage());
        }
    }

    @Override
    public PushResult push(JsonObject json, Action action) {
        return push(json, action, null);
    }

    @Override
    public PushResult push(JsonObject json, Action action, byte[] thumbnail) {
        String id = json.has("id") && json.get("id").isJsonPrimitive() ? json.get("id").getAsString() : null;
        try {
            return register(BundleParser.parse(json), action, thumbnail);
        } catch (BundleException e) {
            return new PushResult(id, false, List.of(), e.getMessage());
        } catch (RuntimeException e) {
            return new PushResult(id, false, List.of(), "Internal error: " + e);
        }
    }

    private PushResult register(Bundle b, Action action, byte[] thumbnail) {
        List<String> warnings = b.warnings;
        String thumb = null;
        if (thumbnail != null) {
            String bad = DevItems.checkThumbnail(thumbnail);
            if (bad == null) thumb = ThumbnailCache.putDev(b.id, thumbnail);
            else {
                warnings = new ArrayList<>(warnings);
                warnings.add("thumbnail ignored: " + bad);
            }
        }
        if (thumb == null) thumb = ThumbnailCache.devKey(b.id); // a re-push without one keeps the last thumbnail
        GpuBundles.prepare(b);
        AvatarItem item = AvatarItem.of(b, ItemSource.DEV, -1, thumb, null, AvatarItems.nextRevision());
        ItemLoader.register(item);
        if (b.isEmote()) {
            if (action == Action.PLAY || action == Action.EQUIP) wardrobe.playEmote(b.id);
        } else if (action == Action.EQUIP) {
            wardrobe.equip(item);
        }
        return new PushResult(b.id, true, warnings, null);
    }

    @Override
    public boolean remove(String id) {
        AvatarItem old = AvatarItems.remove(id, ItemSource.DEV);
        if (old == null) return false;
        if (old.bundle() != null) GpuBundles.evict(old.bundle());
        ThumbnailCache.removeDev(id);
        if (id.equals(wardrobe.playingEmote()) && AvatarItems.get(id) == null) wardrobe.stopEmote();
        return true;
    }

    @Override
    public List<String> ids() {
        List<String> out = new ArrayList<>();
        for (AvatarItem i : AvatarItems.all()) if (i.isDev()) out.add(i.id());
        return out;
    }
}
