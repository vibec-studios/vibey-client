package dev.henny.vui.expr.parse;

public record Token(TokenType type, String text, Object value, int offset) {

    public boolean is(String op) {
        return (type == TokenType.OP || type == TokenType.IDENT) && text.equals(op);
    }

    public boolean isOp(String op) {
        return type == TokenType.OP && text.equals(op);
    }
}
