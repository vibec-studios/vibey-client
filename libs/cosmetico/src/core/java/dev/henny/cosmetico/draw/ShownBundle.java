package dev.henny.cosmetico.draw;

import dev.henny.cosmetico.config.CosmeticoConfig;
import dev.henny.cosmetico.model.Armor;
import dev.henny.cosmetico.model.Attach;
import dev.henny.cosmetico.model.Bone;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemRegistry;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.state.FrameSnapshot;

public final class ShownBundle {
    private final ItemRegistry items;
    private final CosmeticoConfig config;

    public ShownBundle(ItemRegistry items, CosmeticoConfig config) {
        this.items = items;
        this.config = config;
    }

    public Bundle bundle(Avatar a, Slot slot) {
        if (!a.preview && !config.slotVisible(slot)) return null;
        String id = a.shown(slot);
        if (id == null) return null;
        Item item = items.get(id);
        if (item == null || item.isEmote()) return null;
        Bundle b = item.bundle();
        return b != null && b.slot == slot ? b : null;
    }

    public boolean hiddenByArmor(Avatar a, Bundle b) {
        if (!config.hideWithArmor || b.hideWithArmor.isEmpty()) return false;
        FrameSnapshot f = a.frame;
        for (Armor armor : b.hideWithArmor) {
            boolean worn = switch (armor) {
                case HEAD -> f.headArmor;
                case CHEST -> f.chestArmor;
                case LEGS -> f.legsArmor;
                case FEET -> f.feetArmor;
            };
            if (worn) return true;
        }
        return false;
    }

    public static boolean hasWorldBones(Bundle b) {
        for (Bone bone : b.model.bones) if (bone.parent < 0 && bone.attach == Attach.WORLD) return true;
        return false;
    }

    public static int shoulder(Bundle b) {
        if (b.pet == null) return 0;
        return switch (b.pet.follow()) {
            case SHOULDER_RIGHT -> 1;
            case SHOULDER_LEFT -> -1;
            default -> 0;
        };
    }
}
