package dev.henny.vibeyclient.module.visuals.mixin;

import dev.henny.vibeyclient.module.visuals.items.ItemMotion;
import dev.henny.vibeyclient.module.visuals.items.ItemMotionHolder;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin implements ItemMotionHolder {
    @Unique private ItemMotion vibey$motion;

    @Override
    public ItemMotion vibey$motion() {
        if (vibey$motion == null) vibey$motion = new ItemMotion(((ItemEntity) (Object) this).getId());
        return vibey$motion;
    }
}
