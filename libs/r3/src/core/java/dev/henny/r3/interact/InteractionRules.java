package dev.henny.r3.interact;

public final class InteractionRules {
    private boolean interactive = true;
    private boolean movable = true;
    private boolean resizable = true;
    private boolean edgeResizable = true;
    private boolean keepAspect;
    private boolean snapToBlocks = true;
    private boolean turnToViewer = true;
    private boolean distanceAdjustable = true;
    private boolean doubleSided;
    private SizeLimits limits = SizeLimits.DEFAULT;

    public static InteractionRules screen() {
        return new InteractionRules().keepAspect(true);
    }

    public static InteractionRules fixed() {
        return new InteractionRules().movable(false).resizable(false);
    }

    public static InteractionRules hud() {
        return new InteractionRules().snapToBlocks(false).turnToViewer(false).distanceAdjustable(false).doubleSided(true)
                .limits(SizeLimits.HUD_PIXELS);
    }

    public boolean interactive() {
        return interactive;
    }

    public boolean movable() {
        return movable;
    }

    public boolean resizable() {
        return resizable;
    }

    public boolean edgeResizable() {
        return resizable && edgeResizable;
    }

    public boolean keepAspect() {
        return keepAspect;
    }

    public boolean snapToBlocks() {
        return snapToBlocks;
    }

    public boolean turnToViewer() {
        return turnToViewer;
    }

    public boolean distanceAdjustable() {
        return distanceAdjustable;
    }

    public boolean doubleSided() {
        return doubleSided;
    }

    public SizeLimits limits() {
        return limits;
    }

    public InteractionRules interactive(boolean v) {
        interactive = v;
        return this;
    }

    public InteractionRules movable(boolean v) {
        movable = v;
        return this;
    }

    public InteractionRules resizable(boolean v) {
        resizable = v;
        return this;
    }

    public InteractionRules edgeResizable(boolean v) {
        edgeResizable = v;
        return this;
    }

    public InteractionRules keepAspect(boolean v) {
        keepAspect = v;
        return this;
    }

    public InteractionRules snapToBlocks(boolean v) {
        snapToBlocks = v;
        return this;
    }

    public InteractionRules turnToViewer(boolean v) {
        turnToViewer = v;
        return this;
    }

    public InteractionRules distanceAdjustable(boolean v) {
        distanceAdjustable = v;
        return this;
    }

    public InteractionRules doubleSided(boolean v) {
        doubleSided = v;
        return this;
    }

    public InteractionRules limits(SizeLimits v) {
        limits = v;
        return this;
    }
}
