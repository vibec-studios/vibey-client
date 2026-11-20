package dev.henny.companio.expr;

public final class ExprException extends RuntimeException {
    private final int position;

    public ExprException(String message, int position) {
        super(message);
        this.position = position;
    }

    public int position() {
        return position;
    }
}
