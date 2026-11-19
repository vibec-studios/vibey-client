package dev.henny.cosmetico.model;

import java.util.List;

public record BundleHeader(String id, Kind kind, Text name, Text description, String author, List<String> tags, Rarity rarity) {}
