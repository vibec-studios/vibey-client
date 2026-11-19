package dev.henny.cosmetico.parse;

public final class NeedsUpdateException extends RuntimeException {
    public final String required;

    public NeedsUpdateException(String required, String message) {
        super(message, null, false, false);
        this.required = required;
    }
}
