package dev.henny.vibeafk.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Tiny {
    public final Map<String, String> classes = new HashMap<String, String>();
    public final Map<String, String> reverse = new HashMap<String, String>();
    public final Map<String, String> members = new HashMap<String, String>();

    public static Tiny parse(Reader in, String from, String to, Set<String> memberOwners) throws IOException {
        BufferedReader r = in instanceof BufferedReader ? (BufferedReader) in : new BufferedReader(in, 1 << 16);
        String header = r.readLine();
        if (header == null) throw new IOException("empty tiny file");
        String[] h = header.split("\t");
        boolean v2 = h.length >= 3 && h[0].equals("tiny") && h[1].equals("2");
        boolean v1 = h.length >= 2 && h[0].equals("v1");
        if (!v1 && !v2) throw new IOException("unknown tiny header");
        int nsStart = v2 ? 3 : 1;
        int fromIdx = -1;
        int toIdx = -1;
        for (int k = nsStart; k < h.length; k++) {
            if (h[k].equals(from)) fromIdx = k - nsStart;
            if (h[k].equals(to)) toIdx = k - nsStart;
        }
        if (fromIdx < 0 || toIdx < 0) throw new IOException("namespaces " + from + "/" + to + " not in tiny file");
        Tiny t = new Tiny();
        String line;
        String owner = null;
        while ((line = r.readLine()) != null) {
            if (line.isEmpty()) continue;
            String[] p = line.split("\t", -1);
            if (v2) {
                if (!line.startsWith("\t")) {
                    if (p[0].equals("c") && p.length > 1 + Math.max(fromIdx, toIdx)) {
                        owner = p[1 + fromIdx];
                        t.putClass(owner, p[1 + toIdx]);
                    } else {
                        owner = null;
                    }
                } else if (owner != null && p.length > 1 && !line.startsWith("\t\t") && memberOwners.contains(owner)) {
                    if ((p[1].equals("m") || p[1].equals("f")) && p.length > 3 + Math.max(fromIdx, toIdx)) {
                        String desc = p[2];
                        t.members.put(key(owner, p[3 + fromIdx], desc), p[3 + toIdx]);
                    }
                }
            } else {
                if (p[0].equals("CLASS") && p.length > 1 + Math.max(fromIdx, toIdx)) {
                    t.putClass(p[1 + fromIdx], p[1 + toIdx]);
                } else if ((p[0].equals("METHOD") || p[0].equals("FIELD")) && p.length > 3 + Math.max(fromIdx, toIdx)
                        && memberOwners.contains(p[1])) {
                    t.members.put(key(p[1], p[3 + fromIdx], p[2]), p[3 + toIdx]);
                }
            }
        }
        if (fromIdx != 0 && v2) throw new IOException("tiny v2 descriptors must be in the first namespace");
        return t;
    }

    private void putClass(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return;
        classes.put(a, b);
        reverse.put(b, a);
    }

    public static String key(String owner, String name, String desc) {
        return owner + ' ' + name + ' ' + desc;
    }
}
