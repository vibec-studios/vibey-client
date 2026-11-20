package dev.henny.vui.mc.dev;

final class NeoProduction {
    private static final String ENVIRONMENT = "net.neoforged.fml.loading.FMLEnvironment";

    private NeoProduction() {
    }

    static boolean check() {
        try {
            Class<?> env = Class.forName(ENVIRONMENT);
            try {
                return (boolean) env.getMethod("isProduction").invoke(null);
            } catch (NoSuchMethodException e) {
                return env.getField("production").getBoolean(null);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            return true;
        }
    }
}
