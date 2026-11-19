package dev.henny.vui.style;

public final class StyleError extends RuntimeException {
    private final String hint;

    public StyleError(String message, String hint) {
        super(message, null, false, false);
        this.hint = hint;
    }

    public String hint() {
        return hint;
    }
}
