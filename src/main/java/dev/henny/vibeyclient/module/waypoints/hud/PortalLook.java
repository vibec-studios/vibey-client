package dev.henny.vibeyclient.module.waypoints.hud;

import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import dev.henny.vibeyclient.module.waypoints.view.CoordText;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class PortalLook {
    private static BlockPos lastPos;
    private static String lastText = "";

    private PortalLook() {}

    public static String text() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return "";
        BlockPos pos = portalInView(mc);
        if (pos == null) return "";
        if (!pos.equals(lastPos)) {
            lastPos = pos.immutable();
            lastText = CoordText.counterpart(new Pos(pos.getX(), pos.getY(), pos.getZ()), WorldContext.dimension(mc.level));
        }
        return lastText;
    }

    public static void draw(Gfx g) {
        if (Minecraft.getInstance().screen != null) return;
        String s = text();
        if (s.isEmpty()) return;
        int x = g.screenWidth() / 2 - g.width(s) / 2, y = g.screenHeight() / 2 + 10;
        g.rect(x - 3, y - 2, g.width(s) + 6, 12, 0xA0000000);
        g.text(s, x, y, 0xFF3FE0C5);
    }

    private static BlockPos portalInView(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK
                && mc.level.getBlockState(b.getBlockPos()).is(Blocks.NETHER_PORTAL)) return b.getBlockPos();
        BlockPos feet = mc.player.blockPosition();
        if (mc.level.getBlockState(feet).is(Blocks.NETHER_PORTAL)) return feet;
        return null;
    }
}
