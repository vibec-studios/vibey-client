package dev.henny.vui.mc.dev;

public final class DevMode {
    public static final String PROPERTY = "vui.dev";

    private DevMode() {
    }

    public static boolean enabled() {
        String explicit = System.getProperty(PROPERTY);
        if (explicit != null && !explicit.isBlank()) return Boolean.parseBoolean(explicit.trim());
        return loaderDevelopment();
    }

    public static int port() {
        return Integer.getInteger("vui.dev.port", dev.henny.vui.dev.DevBridge.DEFAULT_PORT);
    }

    private static boolean loaderDevelopment() {
        //? if fabric {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        //?} else
        /*return !NeoProduction.check();*/
    }
}
