package dev.henny.vui.expr;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.expr.node.Identifier;
import dev.henny.vui.expr.node.Literal;
import dev.henny.vui.expr.node.MethodRef;
import dev.henny.vui.expr.parse.Parser;
import dev.henny.vui.expr.parse.SyntaxError;

public final class ExprCompiler {
    private ExprCompiler() {
    }

    public static CompiledExpr expression(String source, SourcePos pos, Diagnostics diagnostics) {
        return compile(source, pos, diagnostics, false);
    }

    public static CompiledExpr handler(String source, SourcePos pos, Diagnostics diagnostics) {
        return compile(source, pos, diagnostics, true);
    }

    public static CompiledExpr constant(Object value, SourcePos pos) {
        return new CompiledExpr(String.valueOf(value), pos, new Literal(value), true);
    }

    private static CompiledExpr compile(String source, SourcePos pos, Diagnostics diagnostics, boolean handler) {
        String src = source == null ? "" : source;
        try {
            Expr e = handler ? Parser.parseStatements(src) : Parser.parseExpression(src);
            if (handler && e instanceof Identifier id) e = new MethodRef(id.name());
            return new CompiledExpr(src, pos, e, true);
        } catch (SyntaxError err) {
            diagnostics.error(pos.shift(err.offset()), err.getMessage() + " in '" + src + "'", err.hint());
            return new CompiledExpr(src, pos, new Literal(null), false);
        }
    }
}
