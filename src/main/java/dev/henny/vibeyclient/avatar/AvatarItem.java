package dev.henny.vibeyclient.avatar;

import dev.henny.vibeyclient.avatar.bundle.Bundle;

import java.util.List;

/**
 * One cosmetic or emote as the registry knows it (metadata always, the parsed {@link #bundle()} once loaded).
 * Immutable snapshot: when an item is replaced (hot reload, catalog update) the registry swaps in a new instance with
 * the same {@link #id()}; compare items by id, never by identity.
 */
public final class AvatarItem {
    /** {@link #creator()} of an item whose creator deleted their account (shown localised). */
    public static final String DELETED_CREATOR = "\u0000deleted";

    private final String id;
    private final Bundle.Kind kind;
    private final Slot slot;
    private final Bundle.Text name;
    private final Bundle.Text description;
    private final String author;
    private final List<String> tags;
    private final Rarity rarity;
    private final ItemSource source;
    private final int sort;
    /** Catalog thumbnail URL (absolute) or a classpath resource ("/assets/..."), may be null. */
    private final String thumbnail;
    /** SHA-256 of the bundle bytes, may be null. */
    private final String hash;
    /** "default" | "granted" (catalog) or null. */
    private final String availability;
    private final Bundle bundle;
    private final long revision;
    /** Name of the community creator (catalog `creator.name`), null for official items. */
    private final String creator;
    /** Lowest Vibey Client version that can use the item (catalog/bundle `minClientVersion`), or null. */
    private final String minClientVersion;

    public AvatarItem(String id, Bundle.Kind kind, Slot slot, Bundle.Text name, Bundle.Text description, String author,
                      List<String> tags, Rarity rarity, ItemSource source, int sort, String thumbnail, String hash,
                      String availability, Bundle bundle, long revision) {
        this(id, kind, slot, name, description, author, tags, rarity, source, sort, thumbnail, hash, availability, bundle,
                revision, null, null);
    }

    public AvatarItem(String id, Bundle.Kind kind, Slot slot, Bundle.Text name, Bundle.Text description, String author,
                      List<String> tags, Rarity rarity, ItemSource source, int sort, String thumbnail, String hash,
                      String availability, Bundle bundle, long revision, String creator, String minClientVersion) {
        this.creator = creator == null || creator.isBlank() ? null : creator;
        this.minClientVersion = minClientVersion == null || minClientVersion.isBlank() ? null : minClientVersion;
        this.id = id;
        this.kind = kind;
        this.slot = slot;
        this.name = name == null ? Bundle.Text.EMPTY : name;
        this.description = description == null ? Bundle.Text.EMPTY : description;
        this.author = author == null ? "" : author;
        this.tags = tags == null ? List.of() : List.copyOf(tags);
        this.rarity = rarity == null ? Rarity.COMMON : rarity;
        this.source = source;
        this.sort = sort;
        this.thumbnail = thumbnail;
        this.hash = hash;
        this.availability = availability;
        this.bundle = bundle;
        this.revision = revision;
    }

    public static AvatarItem of(Bundle b, ItemSource source, int sort, String thumbnail, String availability, long revision) {
        return new AvatarItem(b.id, b.kind, b.slot, b.name, b.description, b.author, b.tags, b.rarity, source, sort,
                thumbnail, b.sha256, availability, b, revision);
    }

    public String id() { return id; }
    public Bundle.Kind kind() { return kind; }
    public boolean isEmote() { return kind == Bundle.Kind.EMOTE; }
    /** Null for emotes. */
    public Slot slot() { return slot; }
    /** Localised name (follows the Vibey language). */
    public String name() { return name.get(); }
    public String description() { return description.get(); }
    public Bundle.Text nameText() { return name; }
    public String author() { return author; }
    public List<String> tags() { return tags; }
    public Rarity rarity() { return rarity; }
    public ItemSource source() { return source; }
    public boolean isDev() { return source == ItemSource.DEV; }
    public int sort() { return sort; }
    public String thumbnail() { return thumbnail; }
    public String hash() { return hash; }
    public String availability() { return availability; }
    /** The parsed bundle, or null while it is still downloading/parsing. */
    public Bundle bundle() { return bundle; }
    /** Increases whenever the item (or its bundle) is replaced; for caches. */
    public long revision() { return revision; }
    /** Community creator name ("by …" in the wardrobe), or null. */
    public String creator() { return creator; }
    public String minClientVersion() { return minClientVersion; }
    /** The item needs a newer Vibey Client: shown greyed out with "Update Vibey Client", never rendered. */
    public boolean needsUpdate() { return minClientVersion != null && !ClientVersion.satisfies(minClientVersion); }

    @Override
    public String toString() {
        return id + "[" + source + "]";
    }
}
