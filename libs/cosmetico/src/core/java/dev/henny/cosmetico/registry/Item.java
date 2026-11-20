package dev.henny.cosmetico.registry;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Kind;
import dev.henny.cosmetico.model.Rarity;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.model.Text;
import dev.henny.cosmetico.version.VersionGate;

import java.util.List;

public final class Item {
    public static final String DELETED_CREATOR = "\u0000deleted";

    private final ItemInfo info;
    private final ItemSource source;
    private final ItemMeta meta;
    private final Bundle bundle;
    private final long revision;

    public Item(ItemInfo info, ItemSource source, ItemMeta meta, Bundle bundle, long revision) {
        this.info = info;
        this.source = source;
        this.meta = meta;
        this.bundle = bundle;
        this.revision = revision;
    }

    public static Item of(Bundle b, ItemSource source, int sort, String thumbnail, String availability, long revision) {
        return new Item(ItemInfo.of(b), source, ItemMeta.of(sort, thumbnail, b.sha256, availability), b, revision);
    }

    public String id() { return info.id(); }
    public Kind kind() { return info.kind(); }
    public boolean isEmote() { return info.kind() == Kind.EMOTE; }
    public Slot slot() { return info.slot(); }
    public Text nameText() { return info.name(); }
    public Text descriptionText() { return info.description(); }
    public String name() { return info.name().get(Cosmetico.host().language()); }
    public String description() { return info.description().get(Cosmetico.host().language()); }
    public String author() { return info.author(); }
    public List<String> tags() { return info.tags(); }
    public Rarity rarity() { return info.rarity(); }
    public ItemSource source() { return source; }
    public boolean isDev() { return ItemSource.DEV.equals(source); }
    public ItemMeta meta() { return meta; }
    public int sort() { return meta.sort(); }
    public String thumbnail() { return meta.thumbnail(); }
    public String hash() { return meta.hash(); }
    public String availability() { return meta.availability(); }
    public String creator() { return meta.creator(); }
    public String minClientVersion() { return meta.minClientVersion(); }
    public Bundle bundle() { return bundle; }
    public long revision() { return revision; }

    public boolean needsUpdate(VersionGate gate) {
        return meta.minClientVersion() != null && !gate.satisfies(meta.minClientVersion());
    }

    public boolean needsUpdate() {
        return needsUpdate(Cosmetico.versionGate());
    }

    @Override
    public String toString() {
        return id() + "[" + source.id() + "]";
    }
}
