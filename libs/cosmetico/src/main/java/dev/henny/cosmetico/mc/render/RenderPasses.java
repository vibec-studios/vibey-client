package dev.henny.cosmetico.mc.render;

public final class RenderPasses {
    private RenderPasses() {}

    private static LocatorCollector locators;
    private static AttachedPass attached;
    private static WorldDraw world;

    public static LocatorCollector locators() {
        if (locators == null) locators = new LocatorCollector();
        return locators;
    }

    public static AttachedPass attached() {
        if (attached == null) attached = new AttachedPass(locators());
        return attached;
    }

    public static WorldDraw world() {
        if (world == null) world = new WorldDraw(locators());
        return world;
    }
}
