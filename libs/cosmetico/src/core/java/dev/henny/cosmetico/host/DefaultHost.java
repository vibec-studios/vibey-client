package dev.henny.cosmetico.host;

final class DefaultHost implements CosmeticoHost {
    static final String CONTENT_VERSION = "0.3.1";

    @Override
    public String language() {
        return "en";
    }

    @Override
    public String contentVersion() {
        return CONTENT_VERSION;
    }
}
