package dev.henny.vibeyclient.module.info.core;

import java.util.Set;

public final class HarvestRules {
    private static final ToolKind[] PRIORITY = {ToolKind.PICKAXE, ToolKind.AXE, ToolKind.SHOVEL, ToolKind.HOE,
            ToolKind.SHEARS, ToolKind.SWORD};

    private HarvestRules() {}

    public static ToolKind kind(Set<ToolKind> mineableWith) {
        for (ToolKind k : PRIORITY) if (mineableWith.contains(k)) return k;
        return ToolKind.NONE;
    }

    public static ToolTier tier(boolean requiresTool, boolean needsDiamond, boolean needsIron, boolean needsStone) {
        if (needsDiamond) return ToolTier.DIAMOND;
        if (needsIron) return ToolTier.IRON;
        if (needsStone) return ToolTier.STONE;
        return requiresTool ? ToolTier.WOOD : ToolTier.ANY;
    }

    public static HarvestVerdict verdict(boolean requiresTool, boolean heldCorrect, boolean heldSameKind) {
        if (!requiresTool) return HarvestVerdict.ANY_TOOL;
        if (heldCorrect) return HarvestVerdict.OK;
        return heldSameKind ? HarvestVerdict.TOO_WEAK : HarvestVerdict.WRONG_TOOL;
    }

    public static HarvestInfo of(Set<ToolKind> mineableWith, ToolTier tier, boolean requiresTool, boolean heldCorrect,
                                 boolean heldSameKind) {
        return new HarvestInfo(kind(mineableWith), tier, verdict(requiresTool, heldCorrect, heldSameKind));
    }
}
