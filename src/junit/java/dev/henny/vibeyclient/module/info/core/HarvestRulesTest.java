package dev.henny.vibeyclient.module.info.core;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarvestRulesTest {
    @Test
    void kindFollowsPriority() {
        assertEquals(ToolKind.PICKAXE, HarvestRules.kind(EnumSet.of(ToolKind.PICKAXE, ToolKind.SHOVEL)));
        assertEquals(ToolKind.AXE, HarvestRules.kind(EnumSet.of(ToolKind.AXE)));
        assertEquals(ToolKind.SHEARS, HarvestRules.kind(EnumSet.of(ToolKind.SWORD, ToolKind.SHEARS)));
        assertEquals(ToolKind.NONE, HarvestRules.kind(Set.of()));
    }

    @Test
    void tierFromTags() {
        assertEquals(ToolTier.DIAMOND, HarvestRules.tier(true, true, false, false));
        assertEquals(ToolTier.IRON, HarvestRules.tier(true, false, true, false));
        assertEquals(ToolTier.STONE, HarvestRules.tier(true, false, false, true));
        assertEquals(ToolTier.WOOD, HarvestRules.tier(true, false, false, false));
        assertEquals(ToolTier.ANY, HarvestRules.tier(false, false, false, false));
    }

    @Test
    void verdicts() {
        assertEquals(HarvestVerdict.ANY_TOOL, HarvestRules.verdict(false, false, false));
        assertEquals(HarvestVerdict.OK, HarvestRules.verdict(true, true, true));
        assertEquals(HarvestVerdict.TOO_WEAK, HarvestRules.verdict(true, false, true));
        assertEquals(HarvestVerdict.WRONG_TOOL, HarvestRules.verdict(true, false, false));
    }

    @Test
    void diamondOreWithStonePickaxeIsTooWeak() {
        HarvestInfo info = HarvestRules.of(EnumSet.of(ToolKind.PICKAXE), ToolTier.IRON, true, false, true);
        assertEquals(ToolKind.PICKAXE, info.kind());
        assertEquals(HarvestVerdict.TOO_WEAK, info.verdict());
        assertFalse(info.canHarvest());
        assertTrue(info.showTool());
    }

    @Test
    void dirtByHandIsFine() {
        HarvestInfo info = HarvestRules.of(EnumSet.of(ToolKind.SHOVEL), ToolTier.ANY, false, false, false);
        assertTrue(info.canHarvest());
        assertEquals(ToolKind.SHOVEL, info.kind());
    }
}
