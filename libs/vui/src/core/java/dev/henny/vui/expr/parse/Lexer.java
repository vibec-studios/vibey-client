package dev.henny.vui.expr.parse;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {
    private static final String[] OPERATORS = {
            "===", "!==", "?.", "==", "!=", "<=", ">=", "&&", "||",
            "!", "<", ">", "+", "-", "*", "/", "%", "?", ":", ".", ",", "(", ")", "[", "]", "{", "}", "=", ";"
    };

    private final String src;
    private int i;

    private Lexer(String src) {
        this.src = src;
    }

    public static List<Token> tokenize(String src) {
        Lexer lexer = new Lexer(src);
        List<Token> out = new ArrayList<>();
        while (true) {
            Token t = lexer.next();
            out.add(t);
            if (t.type() == TokenType.EOF) return out;
        }
    }

    private Token next() {
        while (i < src.length() && Character.isWhitespace(src.charAt(i))) i++;
        if (i >= src.length()) return new Token(TokenType.EOF, "", null, i);
        char c = src.charAt(i);
        if (Character.isDigit(c) || (c == '.' && i + 1 < src.length() && Character.isDigit(src.charAt(i + 1)))) return number();
        if (c == '\'' || c == '"' || c == '`') return string(c);
        if (Character.isJavaIdentifierStart(c)) return identifier();
        for (String op : OPERATORS) {
            if (src.startsWith(op, i)) {
                if (op.equals("?.") && i + 2 < src.length() && Character.isDigit(src.charAt(i + 2))) continue;
                Token t = new Token(TokenType.OP, op, null, i);
                i += op.length();
                return t;
            }
        }
        throw new SyntaxError("unexpected character '" + c + "'", i, "expressions support names, numbers, 'strings' and ! && || == != < <= > >= + - * / % ?:");
    }

    private Token number() {
        int start = i;
        while (i < src.length() && (Character.isDigit(src.charAt(i)) || src.charAt(i) == '.' || src.charAt(i) == '_')) i++;
        String text = src.substring(start, i).replace("_", "");
        try {
            return new Token(TokenType.NUMBER, text, Double.parseDouble(text), start);
        } catch (NumberFormatException e) {
            throw new SyntaxError("bad number '" + text + "'", start, null);
        }
    }

    private Token string(char quote) {
        int start = i++;
        StringBuilder sb = new StringBuilder();
        while (i < src.length()) {
            char c = src.charAt(i++);
            if (c == quote) return new Token(TokenType.STRING, src.substring(start, i), sb.toString(), start);
            if (c == '\\' && i < src.length()) {
                char e = src.charAt(i++);
                sb.append(switch (e) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    default -> e;
                });
            } else {
                sb.append(c);
            }
        }
        throw new SyntaxError("unterminated string", start, "add the closing " + quote);
    }

    private Token identifier() {
        int start = i;
        while (i < src.length() && (Character.isJavaIdentifierPart(src.charAt(i)) || src.charAt(i) == '$')) i++;
        return new Token(TokenType.IDENT, src.substring(start, i), null, start);
    }
}
