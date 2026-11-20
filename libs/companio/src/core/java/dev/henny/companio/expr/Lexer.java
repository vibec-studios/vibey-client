package dev.henny.companio.expr;

import java.util.ArrayList;
import java.util.List;

final class Lexer {
    private static final String[] OPS = {"&&", "||", "==", "!=", "<=", ">=", "<", ">", "+", "-", "*", "/", "%", "!"};

    private final String src;
    private int i;

    private Lexer(String src) {
        this.src = src;
    }

    static List<Token> tokens(String src) {
        return new Lexer(src).run();
    }

    private List<Token> run() {
        List<Token> out = new ArrayList<>();
        while (true) {
            while (i < src.length() && Character.isWhitespace(src.charAt(i))) i++;
            if (i >= src.length()) {
                out.add(new Token(TokenKind.END, "", 0, i));
                return out;
            }
            out.add(next());
        }
    }

    private Token next() {
        char c = src.charAt(i);
        int start = i;
        if (Character.isDigit(c) || (c == '.' && i + 1 < src.length() && Character.isDigit(src.charAt(i + 1)))) return number(start);
        if (c == '\'' || c == '"') return string(c, start);
        if (Character.isLetter(c) || c == '_') return name(start);
        switch (c) {
            case '(' -> { i++; return new Token(TokenKind.LPAREN, "(", 0, start); }
            case ')' -> { i++; return new Token(TokenKind.RPAREN, ")", 0, start); }
            case ',' -> { i++; return new Token(TokenKind.COMMA, ",", 0, start); }
            case '?' -> { i++; return new Token(TokenKind.QUESTION, "?", 0, start); }
            case ':' -> { i++; return new Token(TokenKind.COLON, ":", 0, start); }
            default -> { }
        }
        for (String op : OPS) {
            if (src.startsWith(op, i)) {
                i += op.length();
                return new Token(TokenKind.OP, op, 0, start);
            }
        }
        throw new ExprException("unexpected character '" + c + "'", start);
    }

    private Token number(int start) {
        while (i < src.length() && (Character.isDigit(src.charAt(i)) || src.charAt(i) == '.')) i++;
        String text = src.substring(start, i);
        try {
            return new Token(TokenKind.NUMBER, text, Double.parseDouble(text), start);
        } catch (NumberFormatException e) {
            throw new ExprException("bad number \"" + text + "\"", start);
        }
    }

    private Token string(char quote, int start) {
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < src.length() && src.charAt(i) != quote) sb.append(src.charAt(i++));
        if (i >= src.length()) throw new ExprException("text is not closed (missing " + quote + ")", start);
        i++;
        return new Token(TokenKind.STRING, sb.toString(), 0, start);
    }

    private Token name(int start) {
        while (i < src.length()) {
            char c = src.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.') i++;
            else break;
        }
        String text = src.substring(start, i);
        return switch (text) {
            case "and" -> new Token(TokenKind.OP, "&&", 0, start);
            case "or" -> new Token(TokenKind.OP, "||", 0, start);
            case "not" -> new Token(TokenKind.OP, "!", 0, start);
            default -> new Token(TokenKind.NAME, text, 0, start);
        };
    }
}
