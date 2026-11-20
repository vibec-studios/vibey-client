package dev.henny.vibeyclient.module.info.light;

import dev.henny.r3.mc.client.R3Client;
import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.look.LookTarget;
import dev.henny.vibeyclient.module.info.world.WorldLabel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class LightLabel {
    private static final String[] NUMBERS = new String[16];
    private static final WorldLabel LABEL = new WorldLabel(0.03f);

    static {
        for (int i = 0; i < NUMBERS.length; i++) NUMBERS[i] = Integer.toString(i);
    }

    private LightLabel() {}

    public static void tick(InfoModule m, Minecraft mc, boolean forced) {
        BlockPos pos = LookTarget.block();
        Direction face = LookTarget.face();
        if (!wanted(m, mc, forced) || pos == null) {
            LABEL.hide();
            return;
        }
        LightProbe.read(mc.level, pos, face);
        BlockPos at = pos.relative(face);
        double y = face == Direction.UP ? at.getY() + 0.3 : at.getY() + 0.5;
        LABEL.show(NUMBERS[Math.max(0, Math.min(15, LightProbe.blockLight()))], LightProbe.color(),
                at.getX() + 0.5, y, at.getZ() + 0.5);
    }

    private static boolean wanted(InfoModule m, Minecraft mc, boolean forced) {
        if (!m.lightActive() || mc.showOnlyReducedInfo()) return false;
        return forced || !m.lightTorch.on() || LightSources.holding(mc.player);
    }

    public static boolean visible() {
        return R3Client.started() && LABEL.visible();
    }

    public static String value() {
        return LABEL.value();
    }

    public static void clear() {
        if (R3Client.started()) LABEL.remove();
    }
}
