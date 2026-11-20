package dev.henny.vibeyclient.module.cosmetics.vui;

public record CurrentItem(String id, String name, String rarity, String rarityColor, String creator, boolean worn, boolean locked,
                          String lockReason) {}
