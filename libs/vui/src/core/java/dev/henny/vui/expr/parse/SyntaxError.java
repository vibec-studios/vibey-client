package dev.henny.vui.expr.parse;

public final class SyntaxError extends RuntimeException {
    private final int offset;
    private final String hint;

    public SyntaxError(String message, int offset, String hint) {
        super(message, null, false, false);
        this.offset = offset;
        this.hint = hint;
    }

    public int offset() {
        return offset;
    }

    public String hint() {
        return hint;
    }
}
