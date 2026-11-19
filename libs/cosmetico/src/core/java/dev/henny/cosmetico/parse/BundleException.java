package dev.henny.cosmetico.parse;

public final class BundleException extends RuntimeException {
    public BundleException(String message) {
        super(message, null, false, false);
    }
}
