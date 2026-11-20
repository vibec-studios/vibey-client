package dev.henny.companio.expr;

import java.util.ArrayList;
import java.util.List;

final class Parser {
    static final int MAX_DEPTH = 24;
    private static final String[][] LEVELS = {{"||"}, {"&&"}, {"==", "!="}, {"<", "<=", ">", ">="}, {"+", "-"}, {"*", "/", "%"}};

    private final List<Token> tokens;
    private final Names names;
    private int at, depth;

    private Parser(List<Token> tokens, Names names) {
        this.tokens = tokens;
        this.names = names;
    }

    static Node parse(String src, Names names) {
        Parser p = new Parser(Lexer.tokens(src), names);
        Node n = p.ternary();
        Token t = p.peek();
        if (t.kind() != TokenKind.END) throw new ExprException("unexpected \"" + t.text() + "\"", t.pos());
        return n;
    }

    private Token peek() {
        return tokens.get(at);
    }

    private Token take() {
        Token t = tokens.get(at);
        if (t.kind() != TokenKind.END) at++;
        return t;
    }

    private boolean op(String s) {
        Token t = peek();
        if (t.kind() == TokenKind.OP && t.text().equals(s)) {
            at++;
            return true;
        }
        return false;
    }

    private Node ternary() {
        if (++depth > MAX_DEPTH) throw new ExprException("nested too deep (at most " + MAX_DEPTH + ")", peek().pos());
        Node c = binary(0);
        if (peek().kind() == TokenKind.QUESTION) {
            take();
            Node a = ternary();
            Token colon = take();
            if (colon.kind() != TokenKind.COLON) throw new ExprException("\"?\" needs a \":\"", colon.pos());
            c = new Node.Cond(c, a, ternary());
        }
        depth--;
        return c;
    }

    private Node binary(int level) {
        if (level == LEVELS.length) return unary();
        Node a = binary(level + 1);
        while (true) {
            String matched = null;
            for (String o : LEVELS[level]) {
                if (op(o)) {
                    matched = o;
                    break;
                }
            }
            if (matched == null) return a;
            a = new Node.Bin(matched, a, binary(level + 1));
        }
    }

    private Node unary() {
        if (op("!")) return new Node.Not(unary());
        if (op("-")) return new Node.Neg(unary());
        return primary();
    }

    private Node primary() {
        Token t = take();
        return switch (t.kind()) {
            case NUMBER -> new Node.Lit(t.number());
            case STRING -> new Node.Lit(t.text());
            case LPAREN -> group(t);
            case NAME -> name(t);
            case END -> throw new ExprException("expression ends too early", t.pos());
            default -> throw new ExprException("unexpected \"" + t.text() + "\"", t.pos());
        };
    }

    private Node group(Token open) {
        Node n = ternary();
        if (take().kind() != TokenKind.RPAREN) throw new ExprException("missing \")\"", open.pos());
        return n;
    }

    private Node name(Token t) {
        String n = t.text();
        if (n.equals("true")) return new Node.Lit(Boolean.TRUE);
        if (n.equals("false")) return new Node.Lit(Boolean.FALSE);
        if (peek().kind() == TokenKind.LPAREN) return call(t);
        if (!names.known(n)) throw new ExprException("unknown variable \"" + n + "\"" + Suggest.hint(n, names.all()), t.pos());
        return new Node.Var(n);
    }

    private Node call(Token t) {
        String n = t.text();
        int[] ar = Functions.arity(n);
        if (ar == null) throw new ExprException("unknown function \"" + n + "\"" + Suggest.hint(n, Functions.names()), t.pos());
        take();
        List<Node> args = new ArrayList<>();
        if (peek().kind() != TokenKind.RPAREN) {
            args.add(ternary());
            while (peek().kind() == TokenKind.COMMA) {
                take();
                args.add(ternary());
            }
        }
        if (take().kind() != TokenKind.RPAREN) throw new ExprException("missing \")\" after the arguments of " + n, t.pos());
        if (args.size() < ar[0] || args.size() > ar[1]) {
            String want = ar[0] == ar[1] ? Integer.toString(ar[0]) : ar[0] + " to " + ar[1];
            throw new ExprException(n + "() takes " + want + " argument(s), got " + args.size(), t.pos());
        }
        return new Node.Call(n, args.toArray(new Node[0]));
    }
}
