package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.cosmetico.registry.Item;

public record ItemRow(Item item, String id, String name, String tip, String thumb, String rarityColor, boolean worn,
                      boolean locked, boolean selected) {}
