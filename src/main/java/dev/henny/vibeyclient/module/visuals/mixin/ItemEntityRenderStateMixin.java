package dev.henny.vibeyclient.module.visuals.mixin;

import dev.henny.vibeyclient.module.visuals.items.ItemPose;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
abstract class ItemEntityRenderStateMixin implements ItemPose {
    @Unique private boolean vibey$active;
    @Unique private float vibey$yaw;
    @Unique private float vibey$rotX;
    @Unique private float vibey$lift;
    @Unique private float vibey$centerY;
    @Unique private float vibey$centerZ;

    @Override
    public void vibey$pose(boolean active, float yaw, float rotX, float lift, float centerY, float centerZ) {
        vibey$active = active;
        vibey$yaw = yaw;
        vibey$rotX = rotX;
        vibey$lift = lift;
        vibey$centerY = centerY;
        vibey$centerZ = centerZ;
    }

    @Override
    public boolean vibey$active() {
        return vibey$active;
    }

    @Override
    public float vibey$yaw() {
        return vibey$yaw;
    }

    @Override
    public float vibey$rotX() {
        return vibey$rotX;
    }

    @Override
    public float vibey$lift() {
        return vibey$lift;
    }

    @Override
    public float vibey$centerY() {
        return vibey$centerY;
    }

    @Override
    public float vibey$centerZ() {
        return vibey$centerZ;
    }
}
