package dev.henny.vui.template;

import java.util.Collection;
import java.util.Set;

final class TagSuggestions {
    private TagSuggestions() {
    }

    static String suggest(String tag, Collection<String> builtIn, Set<String> imported) {
        String best = null;
        int bestScore = Integer.MAX_VALUE;
        for (String c : builtIn) {
            int d = distance(tag, c);
            if (d < bestScore) {
                bestScore = d;
                best = c;
            }
        }
        for (String c : imported) {
            int d = distance(tag, c);
            if (d < bestScore) {
                bestScore = d;
                best = c;
            }
        }
        if (best != null && bestScore <= 2) return "did you mean <" + best + ">?";
        return "import it with <import name=\"" + tag + "\" src=\"ns:path\"/> or use a built-in element";
    }

    private static int distance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }
        return d[a.length()][b.length()];
    }
}
