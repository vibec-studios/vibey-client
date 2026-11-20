package dev.henny.companio.expr;

import java.util.Collection;

public final class Suggest {
    private Suggest() {}

    public static String closest(String word, Collection<String> options) {
        String best = null;
        int bestD = Integer.MAX_VALUE;
        for (String o : options) {
            int d = distance(word, o);
            if (d < bestD) {
                bestD = d;
                best = o;
            }
        }
        return best != null && bestD <= Math.max(2, word.length() / 3) ? best : null;
    }

    public static String hint(String word, Collection<String> options) {
        String c = closest(word, options);
        return c == null ? "" : ", did you mean \"" + c + "\"?";
    }

    static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }
}
