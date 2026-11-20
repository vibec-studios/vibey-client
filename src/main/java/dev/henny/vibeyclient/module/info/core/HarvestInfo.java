package dev.henny.vibeyclient.module.info.core;

public record HarvestInfo(ToolKind kind, ToolTier tier, HarvestVerdict verdict) {
    public boolean canHarvest() {
        return verdict == HarvestVerdict.ANY_TOOL || verdict == HarvestVerdict.OK;
    }

    public boolean showTool() {
        return kind != ToolKind.NONE;
    }
}
