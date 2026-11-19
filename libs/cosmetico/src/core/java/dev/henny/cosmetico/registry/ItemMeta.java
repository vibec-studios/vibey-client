package dev.henny.cosmetico.registry;

public record ItemMeta(int sort, String thumbnail, String hash, String availability, String creator, String minClientVersion) {
    public static ItemMeta of(int sort, String thumbnail, String hash, String availability) {
        return new ItemMeta(sort, thumbnail, hash, availability, null, null);
    }

    public ItemMeta {
        creator = creator == null || creator.isBlank() ? null : creator;
        minClientVersion = minClientVersion == null || minClientVersion.isBlank() ? null : minClientVersion;
    }
}
