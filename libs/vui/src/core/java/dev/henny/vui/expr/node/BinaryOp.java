package dev.henny.vui.expr.node;

public enum BinaryOp {
    ADD("+"), SUB("-"), MUL("*"), DIV("/"), MOD("%"),
    EQ("=="), NE("!="), LT("<"), LE("<="), GT(">"), GE(">=");

    private final String symbol;

    BinaryOp(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public static BinaryOp of(String symbol) {
        for (BinaryOp op : values()) if (op.symbol.equals(symbol)) return op;
        if (symbol.equals("===")) return EQ;
        if (symbol.equals("!==")) return NE;
        return null;
    }
}
