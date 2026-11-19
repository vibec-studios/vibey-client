package dev.henny.vui.diag;

public final class Suggest {
    private Suggest() {
    }

    public static String closest(String input, Iterable<String> candidates, int maxDistance) {
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (String c : candidates) {
            int d = distance(input, c);
            if (d < bestDistance) {
                bestDistance = d;
                best = c;
            }
        }
        return bestDistance <= maxDistance ? best : null;
    }

    public static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
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
