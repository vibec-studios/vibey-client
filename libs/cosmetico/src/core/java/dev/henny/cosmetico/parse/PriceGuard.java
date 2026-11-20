package dev.henny.cosmetico.parse;

import com.google.gson.JsonObject;

final class PriceGuard {
    private PriceGuard() {}

    static void check(JsonObject o) {
        for (String k : new String[]{"price", "cost", "prices"}) if (o.has(k)) throw new BundleException("\"" + k + "\" is not allowed");
    }
}
