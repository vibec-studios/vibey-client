package dev.henny.cosmetico.mc.player;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.config.CosmeticoConfig;
import dev.henny.cosmetico.draw.SlotVisibility;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.state.FrameSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public final class PlayerSnapshot {
    private PlayerSnapshot() {}

    public static Avatar extract(Player p, float partial) {
        if (!Cosmetico.active()) return null;
        Avatar a = Cosmetico.engine().avatars.peek(p.getUUID());
        if (a == null) return null;
        Minecraft mc = Minecraft.getInstance();
        CosmeticoConfig config = Cosmetico.config();
        boolean self = p == mc.player || config.isSelf(p.getUUID());
        FrameSnapshot f = a.frame;
        f.partial = partial;
        f.x = p.xo + (p.getX() - p.xo) * partial;
        f.y = p.yo + (p.getY() - p.yo) * partial;
        f.z = p.zo + (p.getZ() - p.zo) * partial;
        f.bodyYaw = p.yBodyRotO + wrap(p.yBodyRot - p.yBodyRotO) * partial;
        f.headArmor = !p.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        f.chestArmor = !p.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
        f.legsArmor = !p.getItemBySlot(EquipmentSlot.LEGS).isEmpty();
        f.feetArmor = !p.getItemBySlot(EquipmentSlot.FEET).isEmpty();
        f.crouching = p.isCrouching();
        f.fallFlying = p.isFallFlying();
        boolean invisible = mc.player != null ? p.isInvisibleTo(mc.player) : p.isInvisible();
        f.hidden = SlotVisibility.hidden(config, self, invisible, p.isSpectator());
        return a;
    }

    static float wrap(float deg) {
        return ((deg % 360f) + 540f) % 360f - 180f;
    }
}
