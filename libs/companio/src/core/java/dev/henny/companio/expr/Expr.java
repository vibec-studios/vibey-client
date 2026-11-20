package dev.henny.companio.expr;

public final class Expr {
    public static final int MAX_LENGTH = 400;
    public static final Expr TRUE = new Expr("true", new Node.Lit(Boolean.TRUE));
    public static final Expr FALSE = new Expr("false", new Node.Lit(Boolean.FALSE));

    private final String source;
    private final Node root;

    private Expr(String source, Node root) {
        this.source = source;
        this.root = root;
    }

    public static Expr compile(String source, Names names) {
        if (source.length() > MAX_LENGTH) throw new ExprException("too long (" + source.length() + " characters, at most " + MAX_LENGTH + ")", 0);
        if (source.isBlank()) throw new ExprException("empty expression", 0);
        return new Expr(source, Parser.parse(source, names));
    }

    public static Expr constant(Object value) {
        return new Expr(Values.text(value), new Node.Lit(value));
    }

    public String source() {
        return source;
    }

    public Object eval(Scope scope) {
        return root.eval(scope);
    }

    public boolean test(Scope scope) {
        return Values.truthy(root.eval(scope));
    }

    public double number(Scope scope) {
        return Values.number(root.eval(scope));
    }

    @Override
    public String toString() {
        return source;
    }
}
