package dev.henny.vibeyclient.module.visuals.effects;

import dev.henny.vibeyclient.module.visuals.effects.core.PreviewCurve;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class EffectsPreview {
    public static final long FIRE_MS = 3000, PUMPKIN_MS = 3000, NAUSEA_MS = 3500;
    public static final String FIRE = "fire", PUMPKIN = "pumpkin", NAUSEA = "nausea", HURT = "hurt", TOTEM = "totem",
            BLAST = "blast";

    private static long fire, pumpkin, nausea;

    private EffectsPreview() {}

    public static void run(String kind) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        switch (kind) {
            case FIRE -> fire = now();
            case PUMPKIN -> pumpkin = now();
            case NAUSEA -> nausea = now();
            case HURT -> mc.player.animateHurt(mc.player.getYRot() + (float) (Math.random() * 120 - 60));
            case TOTEM -> totem(mc);
            case BLAST -> blast(mc);
            default -> {
            }
        }
    }

    public static boolean fireOn() {
        return PreviewCurve.active(fire, FIRE_MS, now());
    }

    public static boolean pumpkinOn() {
        return PreviewCurve.active(pumpkin, PUMPKIN_MS, now());
    }

    public static float nausea() {
        return nausea == 0 ? 0 : PreviewCurve.level(nausea, NAUSEA_MS, now());
    }

    public static void clear() {
        fire = pumpkin = nausea = 0;
    }

    private static void totem(Minecraft mc) {
        mc.gameRenderer.displayItemActivation(new ItemStack(Items.TOTEM_OF_UNDYING));
        mc.particleEngine.createTrackingEmitter(mc.player, ParticleTypes.TOTEM_OF_UNDYING, 30);
    }

    private static void blast(Minecraft mc) {
        Vec3 at = PreviewSpot.ahead(7);
        mc.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0);
        mc.level.playLocalSound(at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.5f, 1f, false);
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
