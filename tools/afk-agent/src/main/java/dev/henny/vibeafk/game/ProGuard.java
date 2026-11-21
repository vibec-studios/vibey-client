package dev.henny.vibeafk.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProGuard {
    public final Map<String, String> mojToObf = new HashMap<String, String>();
    public final Map<String, String> obfToMoj = new HashMap<String, String>();
    public final Map<String, Map<String, Field>> fields = new HashMap<String, Map<String, Field>>();
    public final Map<String, List<Method>> methods = new HashMap<String, List<Method>>();

    public static final class Field {
        public final String type;
        public final String obf;

        Field(String type, String obf) {
            this.type = type;
            this.obf = obf;
        }
    }

    public static final class Method {
        public final String name;
        public final String ret;
        public final String[] params;
        public final String obf;

        Method(String name, String ret, String[] params, String obf) {
            this.name = name;
            this.ret = ret;
            this.params = params;
            this.obf = obf;
        }
    }

    public static ProGuard parse(Reader in, Set<String> memberClasses) throws IOException {
        ProGuard p = new ProGuard();
        BufferedReader r = in instanceof BufferedReader ? (BufferedReader) in : new BufferedReader(in, 1 << 16);
        String line;
        String current = null;
        boolean keep = false;
        int lines = 0;
        while ((line = r.readLine()) != null) {
            lines++;
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            if (line.charAt(0) != ' ') {
                int arrow = line.indexOf(" -> ");
                if (arrow < 0 || !line.endsWith(":")) {
                    current = null;
                    keep = false;
                    continue;
                }
                current = line.substring(0, arrow).trim();
                String obf = line.substring(arrow + 4, line.length() - 1).trim();
                p.mojToObf.put(current, obf);
                p.obfToMoj.put(obf, current);
                keep = memberClasses == null || memberClasses.contains(current);
                continue;
            }
            if (!keep || current == null) continue;
            String t = line.trim();
            int arrow = t.lastIndexOf(" -> ");
            if (arrow < 0) continue;
            String obf = t.substring(arrow + 4).trim();
            String left = t.substring(0, arrow);
            int paren = left.indexOf('(');
            if (paren < 0) {
                int sp = left.lastIndexOf(' ');
                if (sp < 0) continue;
                Map<String, Field> m = p.fields.get(current);
                if (m == null) p.fields.put(current, m = new HashMap<String, Field>());
                m.put(left.substring(sp + 1), new Field(left.substring(0, sp), obf));
            } else {
                String head = left.substring(0, paren);
                int close = left.indexOf(')', paren);
                if (close < 0) continue;
                String paramStr = left.substring(paren + 1, close);
                while (!head.isEmpty() && (Character.isDigit(head.charAt(0)) || head.charAt(0) == ':')) head = head.substring(1);
                int sp = head.lastIndexOf(' ');
                if (sp < 0) continue;
                String ret = head.substring(0, sp);
                String name = head.substring(sp + 1);
                String[] params = paramStr.isEmpty() ? new String[0] : paramStr.split(",");
                List<Method> l = p.methods.get(current);
                if (l == null) p.methods.put(current, l = new ArrayList<Method>());
                l.add(new Method(name, ret, params, obf));
            }
        }
        if (lines > 0 && p.mojToObf.isEmpty()) throw new IOException("no class mappings");
        return p;
    }
}
