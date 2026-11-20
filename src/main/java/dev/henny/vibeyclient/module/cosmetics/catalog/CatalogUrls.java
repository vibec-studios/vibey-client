package dev.henny.vibeyclient.module.cosmetics.catalog;

import dev.henny.vibeyclient.api.GlobalSettings;

final class CatalogUrls {
    private CatalogUrls() {}

    static final String CATALOG_PATH = "/api/v1/cosmetics/catalog";

    static String base() {
        String b = GlobalSettings.backend();
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    static String absolute(String url) {
        if (url == null || url.isEmpty()) return null;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return base() + (url.startsWith("/") ? url : "/" + url);
    }
}
