package dev.henny.vibeyclient.module.visuals.particles;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class PreviewOptions {
    private static final int PURPLE = 0xFF8B6CFF;

    private PreviewOptions() {}

    @SuppressWarnings("unchecked")
    public static ParticleOptions of(ParticleType<?> type) {
        if (type == null) return null;
        if (type instanceof SimpleParticleType simple) return simple;
        if (type == ParticleTypes.BLOCK || type == ParticleTypes.BLOCK_MARKER || type == ParticleTypes.FALLING_DUST
                || type == ParticleTypes.DUST_PILLAR || type == ParticleTypes.BLOCK_CRUMBLE) {
            return new BlockParticleOption((ParticleType<BlockParticleOption>) type, Blocks.GRASS_BLOCK.defaultBlockState());
        }
        if (type == ParticleTypes.ITEM) return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.DIAMOND));
        if (type == ParticleTypes.DUST) return new DustParticleOptions(0xFF3B3B, 1f);
        if (type == ParticleTypes.DUST_COLOR_TRANSITION) return new DustColorTransitionOptions(0xFF3B3B, 0x3FE0C5, 1f);
        if (type == ParticleTypes.DRAGON_BREATH) return PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1f);
        if (type == ParticleTypes.EFFECT || type == ParticleTypes.INSTANT_EFFECT) {
            return SpellParticleOption.create((ParticleType<SpellParticleOption>) type, PURPLE, 1f);
        }
        if (type == ParticleTypes.ENTITY_EFFECT || type == ParticleTypes.TINTED_LEAVES || type == ParticleTypes.FLASH) {
            return ColorParticleOption.create((ParticleType<ColorParticleOption>) type, PURPLE);
        }
        if (type == ParticleTypes.SCULK_CHARGE) return new SculkChargeParticleOptions(0f);
        if (type == ParticleTypes.SHRIEK) return new ShriekParticleOption(0);
        return null;
    }
}
