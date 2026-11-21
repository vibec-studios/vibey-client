package dev.henny.vibeyclient.client.branding;

public final class ClientBrand {
    public static final String ID = "vibey";
    private static final String VANILLA = "vanilla";
    private static final char SEPARATOR = ',';

    private ClientBrand() {}

    public static String of(String loaderBrand, boolean show) {
        if (!show) return loaderBrand;
        String base = loaderBrand == null ? "" : loaderBrand.trim();
        if (base.equals(ID) || base.startsWith(ID + SEPARATOR)) return base;
        if (base.isEmpty() || base.equals(VANILLA)) return ID;
        return ID + SEPARATOR + base;
    }

    public static String current(String loaderBrand) {
        return of(loaderBrand, BrandSetting.show());
    }
}
