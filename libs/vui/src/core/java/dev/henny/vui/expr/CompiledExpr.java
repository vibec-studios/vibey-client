package dev.henny.vui.expr;

import dev.henny.vui.diag.ErrorSink;
import dev.henny.vui.diag.SourcePos;

public final class CompiledExpr {
    private final String source;
    private final SourcePos pos;
    private final Expr expr;
    private final boolean valid;

    public CompiledExpr(String source, SourcePos pos, Expr expr, boolean valid) {
        this.source = source;
        this.pos = pos;
        this.expr = expr;
        this.valid = valid;
    }

    public String source() {
        return source;
    }

    public SourcePos pos() {
        return pos;
    }

    public Expr expr() {
        return expr;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isConstant() {
        return expr.isConstant();
    }

    public boolean isAssignable() {
        return expr.isAssignable();
    }

    public Object evaluate(Scope scope, ErrorSink errors) {
        if (!valid) return null;
        try {
            Object v = expr.eval(scope);
            return v == Scope.MISSING ? null : v;
        } catch (EvalException e) {
            errors.report(pos, e.getMessage() + " in '" + source + "'", e.hint());
        } catch (RuntimeException e) {
            errors.report(pos, "error in '" + source + "': " + e, null);
        }
        return null;
    }

    public void assign(Scope scope, Object value, ErrorSink errors) {
        if (!valid) return;
        try {
            expr.assign(scope, value);
        } catch (EvalException e) {
            errors.report(pos, e.getMessage() + " in '" + source + "'", e.hint());
        } catch (RuntimeException e) {
            errors.report(pos, "error writing '" + source + "': " + e, null);
        }
    }

    @Override
    public String toString() {
        return source;
    }
}
