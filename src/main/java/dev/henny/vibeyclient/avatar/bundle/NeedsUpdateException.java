package dev.henny.vibeyclient.avatar.bundle;

/**
 * A bundle written for a newer Vibey Client ({@code minClientVersion} above ours, or a newer {@code formatVersion}).
 * Not an error of the bundle: catalog items like this are listed greyed out with "Update Vibey Client".
 */
public final class NeedsUpdateException extends RuntimeException {
    /** The version the bundle asks for, or null if unknown (newer format). */
    public final String required;

    public NeedsUpdateException(String required, String message) {
        super(message, null, false, false);
        this.required = required;
    }
}
