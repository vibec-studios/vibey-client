package dev.henny.cosmetico.model;

import java.util.List;
import java.util.Set;

public record BundleLooks(Slot slot, Set<Armor> hideWithArmor, PetOptions pet, int capeTexture, int shieldTexture,
                          EmoteOptions emote, List<OffsetRule> offsetWhen) {}
