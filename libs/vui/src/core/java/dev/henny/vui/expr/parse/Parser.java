package dev.henny.vui.expr.parse;

import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.node.ArrayLiteral;
import dev.henny.vui.expr.node.Assign;
import dev.henny.vui.expr.node.Binary;
import dev.henny.vui.expr.node.BinaryOp;
import dev.henny.vui.expr.node.CallFunction;
import dev.henny.vui.expr.node.CallMethod;
import dev.henny.vui.expr.node.Conditional;
import dev.henny.vui.expr.node.Identifier;
import dev.henny.vui.expr.node.Index;
import dev.henny.vui.expr.node.Literal;
import dev.henny.vui.expr.node.Logical;
import dev.henny.vui.expr.node.Member;
import dev.henny.vui.expr.node.ObjectLiteral;
import dev.henny.vui.expr.node.Sequence;
import dev.henny.vui.expr.node.Unary;

import java.util.ArrayList;
import java.util.List;

public final class Parser {
    private final List<Token> tokens;
    private final boolean statements;
    private int p;

    private Parser(List<Token> tokens, boolean statements) {
        this.tokens = tokens;
        this.statements = statements;
    }

    public static Expr parseExpression(String src) {
        return new Parser(Lexer.tokenize(src), false).root();
    }

    public static Expr parseStatements(String src) {
        return new Parser(Lexer.tokenize(src), true).root();
    }

    private Expr root() {
        if (peek().type() == TokenType.EOF) throw new SyntaxError("empty expression", 0, "write a value, a name or a call");
        Expr e = statements ? sequence() : conditional();
        if (peek().type() != TokenType.EOF) {
            Token t = peek();
            throw new SyntaxError("unexpected '" + t.text() + "'", t.offset(), statements ? "separate statements with ;" : "an operator may be missing");
        }
        return e;
    }

    private Expr sequence() {
        List<Expr> items = new ArrayList<>();
        items.add(assignment());
        while (match(";")) {
            if (peek().type() == TokenType.EOF) break;
            items.add(assignment());
        }
        return items.size() == 1 ? items.get(0) : new Sequence(items.toArray(new Expr[0]));
    }

    private Expr assignment() {
        Token start = peek();
        Expr target = conditional();
        if (!match("=")) return target;
        if (!target.isAssignable()) throw new SyntaxError("cannot assign to this", start.offset(), "the left side of = must be a name or a path");
        return new Assign(target, assignment());
    }

    private Expr conditional() {
        Expr test = or();
        if (!match("?")) return test;
        Expr then = conditional();
        expect(":", "a ?: needs both branches, e.g. a ? b : c");
        return new Conditional(test, then, conditional());
    }

    private Expr or() {
        Expr left = and();
        while (match("||")) left = new Logical(false, left, and());
        return left;
    }

    private Expr and() {
        Expr left = equality();
        while (match("&&")) left = new Logical(true, left, equality());
        return left;
    }

    private Expr equality() {
        Expr left = relational();
        while (peekOp("==", "!=", "===", "!==")) left = new Binary(BinaryOp.of(advance().text()), left, relational());
        return left;
    }

    private Expr relational() {
        Expr left = additive();
        while (peekOp("<", "<=", ">", ">=")) left = new Binary(BinaryOp.of(advance().text()), left, additive());
        return left;
    }

    private Expr additive() {
        Expr left = multiplicative();
        while (peekOp("+", "-")) left = new Binary(BinaryOp.of(advance().text()), left, multiplicative());
        return left;
    }

    private Expr multiplicative() {
        Expr left = unary();
        while (peekOp("*", "/", "%")) left = new Binary(BinaryOp.of(advance().text()), left, unary());
        return left;
    }

    private Expr unary() {
        if (peekOp("!", "-", "+")) {
            char op = advance().text().charAt(0);
            return new Unary(op, unary());
        }
        return postfix(primary());
    }

    private Expr postfix(Expr e) {
        while (true) {
            if (peekOp(".", "?.")) {
                boolean optional = advance().text().equals("?.");
                Token name = advance();
                if (name.type() != TokenType.IDENT) throw new SyntaxError("expected a name after '.'", name.offset(), null);
                if (match("(")) e = new CallMethod(e, name.text(), arguments());
                else e = new Member(e, name.text(), optional);
            } else if (match("[")) {
                Expr index = conditional();
                expect("]", "close the index with ]");
                e = new Index(e, index);
            } else if (peekOp("(") && e instanceof Identifier id) {
                advance();
                e = new CallFunction(id.name(), arguments());
            } else {
                return e;
            }
        }
    }

    private Expr[] arguments() {
        List<Expr> args = new ArrayList<>();
        if (!match(")")) {
            do args.add(conditional()); while (match(","));
            expect(")", "close the call with )");
        }
        return args.toArray(new Expr[0]);
    }

    private Expr primary() {
        Token t = advance();
        switch (t.type()) {
            case NUMBER, STRING:
                return new Literal(t.value());
            case IDENT:
                return switch (t.text()) {
                    case "true" -> new Literal(Boolean.TRUE);
                    case "false" -> new Literal(Boolean.FALSE);
                    case "null", "undefined" -> new Literal(null);
                    default -> new Identifier(t.text());
                };
            case EOF:
                throw new SyntaxError("unexpected end of expression", t.offset(), "something is missing at the end");
            default:
                break;
        }
        if (t.isOp("(")) {
            Expr inner = conditional();
            expect(")", "close the bracket with )");
            return inner;
        }
        if (t.isOp("{")) return object();
        if (t.isOp("[")) return array();
        throw new SyntaxError("unexpected '" + t.text() + "'", t.offset(), null);
    }

    private Expr object() {
        List<String> keys = new ArrayList<>();
        List<Expr> values = new ArrayList<>();
        if (!match("}")) {
            do {
                if (peekOp("}")) break;
                Token key = advance();
                if (key.type() != TokenType.IDENT && key.type() != TokenType.STRING) {
                    throw new SyntaxError("expected a key", key.offset(), "object keys are names or 'strings'");
                }
                String name = key.type() == TokenType.STRING ? (String) key.value() : key.text();
                keys.add(name);
                values.add(match(":") ? conditional() : new Identifier(name));
            } while (match(","));
            expect("}", "close the object with }");
        }
        return new ObjectLiteral(keys.toArray(new String[0]), values.toArray(new Expr[0]));
    }

    private Expr array() {
        List<Expr> items = new ArrayList<>();
        if (!match("]")) {
            do items.add(conditional()); while (match(","));
            expect("]", "close the list with ]");
        }
        return new ArrayLiteral(items.toArray(new Expr[0]));
    }

    private Token peek() {
        return tokens.get(p);
    }

    private Token advance() {
        Token t = tokens.get(p);
        if (t.type() != TokenType.EOF) p++;
        return t;
    }

    private boolean peekOp(String... ops) {
        Token t = peek();
        if (t.type() != TokenType.OP) return false;
        for (String op : ops) if (t.text().equals(op)) return true;
        return false;
    }

    private boolean match(String op) {
        if (!peekOp(op)) return false;
        p++;
        return true;
    }

    private void expect(String op, String hint) {
        if (!match(op)) {
            Token t = peek();
            throw new SyntaxError("expected '" + op + "' but found " + (t.type() == TokenType.EOF ? "the end" : "'" + t.text() + "'"), t.offset(), hint);
        }
    }
}
