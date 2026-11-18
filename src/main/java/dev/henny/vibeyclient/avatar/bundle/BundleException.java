package dev.henny.vibeyclient.avatar.bundle;

/** A .vibey bundle that does not pass validation (message is shown to creators). */
public final class BundleException extends RuntimeException {
    public BundleException(String message) {
        super(message, null, false, false);
    }
}
