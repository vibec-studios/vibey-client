package dev.henny.vui.expr;

public final class EvalException extends RuntimeException {
    private final String hint;

    public EvalException(String message, String hint) {
        super(message, null, false, false);
        this.hint = hint;
    }

    public EvalException(String message) {
        this(message, null);
    }

    public String hint() {
        return hint;
    }
}
