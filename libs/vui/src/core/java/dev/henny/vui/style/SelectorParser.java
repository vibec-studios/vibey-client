package dev.henny.vui.style;

import java.util.ArrayList;
import java.util.List;

public final class SelectorParser {
    private final String src;
    private int i;

    private SelectorParser(String src) {
        this.src = src;
    }

    public static Selector parse(String text) {
        return new SelectorParser(text.trim()).selector();
    }

    private Selector selector() {
        if (src.isEmpty()) throw new StyleError("empty selector", null);
        List<CompoundSelector> parts = new ArrayList<>();
        List<Character> combinators = new ArrayList<>();
        parts.add(compound());
        while (i < src.length()) {
            boolean space = skipSpaces();
            if (i >= src.length()) break;
            char c = src.charAt(i);
            if (c == '>' || c == '+' || c == '~') {
                i++;
                skipSpaces();
                combinators.add(c == '>' ? Selector.CHILD : c == '+' ? Selector.ADJACENT : Selector.SIBLING);
            } else if (space) {
                combinators.add(Selector.DESCENDANT);
            } else {
                throw new StyleError("unexpected '" + c + "' in selector '" + src + "'", null);
            }
            parts.add(compound());
        }
        char[] comb = new char[combinators.size()];
        for (int k = 0; k < comb.length; k++) comb[k] = combinators.get(k);
        return new Selector(parts.toArray(new CompoundSelector[0]), comb, src);
    }

    private CompoundSelector compound() {
        String tag = null;
        String id = null;
        List<String> classes = new ArrayList<>();
        int states = 0;
        int notStates = 0;
        boolean universal = false;
        if (i < src.length() && src.charAt(i) == '*') {
            i++;
            universal = true;
        }
        else if (i < src.length() && isNameChar(src.charAt(i))) tag = name();
        while (i < src.length()) {
            char c = src.charAt(i);
            if (c == '.') {
                i++;
                classes.add(name());
            } else if (c == '#') {
                i++;
                id = name();
            } else if (c == ':') {
                i++;
                if (src.charAt(i) == ':') i++;
                String pseudo = name();
                if (pseudo.equals("not")) notStates |= notPseudo();
                else states |= pseudoFlag(pseudo);
            } else {
                break;
            }
        }
        if (!universal && tag == null && id == null && classes.isEmpty() && states == 0 && notStates == 0) {
            throw new StyleError("bad selector '" + src + "'", "use tag, .class, #id and :hover style pseudo classes");
        }
        return new CompoundSelector(tag, id, classes.toArray(new String[0]), states, notStates);
    }

    private int notPseudo() {
        if (i >= src.length() || src.charAt(i) != '(') throw new StyleError(":not needs brackets", "e.g. :not(:disabled)");
        int close = src.indexOf(')', i);
        if (close < 0) throw new StyleError("unclosed :not(", null);
        String inner = src.substring(i + 1, close).trim();
        i = close + 1;
        if (!inner.startsWith(":")) throw new StyleError(":not() supports pseudo classes only", "e.g. :not(:disabled)");
        return pseudoFlag(inner.substring(1));
    }

    private static int pseudoFlag(String name) {
        int flag = StateFlags.byName(name);
        if (flag == 0) {
            throw new StyleError("unknown pseudo class :" + name,
                    "known: :hover :focus :active :disabled :checked :selected :open :enter :leave :first-child :last-child :focus-within");
        }
        return flag;
    }

    private String name() {
        int start = i;
        while (i < src.length() && isNameChar(src.charAt(i))) i++;
        if (start == i) throw new StyleError("expected a name in selector '" + src + "'", null);
        return src.substring(start, i);
    }

    private boolean skipSpaces() {
        boolean any = false;
        while (i < src.length() && Character.isWhitespace(src.charAt(i))) {
            i++;
            any = true;
        }
        return any;
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '_';
    }
}
