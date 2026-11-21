package dev.henny.vibeafk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
    }

    public static Object parse(String text) {
        if (text == null) throw new IllegalArgumentException("null");
        Json p = new Json(text);
        p.ws();
        Object v = p.value(0);
        p.ws();
        if (p.i != p.s.length()) throw p.err("trailing data");
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object o = parse(text);
        if (!(o instanceof Map)) throw new IllegalArgumentException("not an object");
        return (Map<String, Object>) o;
    }

    private IllegalArgumentException err(String m) {
        return new IllegalArgumentException(m + " at " + i);
    }

    private void ws() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '﻿') i++;
            else break;
        }
    }

    private Object value(int depth) {
        if (depth > 64) throw err("too deep");
        if (i >= s.length()) throw err("unexpected end");
        char c = s.charAt(i);
        switch (c) {
            case '{': return object(depth);
            case '[': return array(depth);
            case '"': return string();
            case 't': return word("true", Boolean.TRUE);
            case 'f': return word("false", Boolean.FALSE);
            case 'n': return word("null", null);
            default: return number();
        }
    }

    private Object word(String w, Object v) {
        if (!s.startsWith(w, i)) throw err("bad literal");
        i += w.length();
        return v;
    }

    private Map<String, Object> object(int depth) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        i++;
        ws();
        if (peek() == '}') {
            i++;
            return m;
        }
        while (true) {
            ws();
            if (peek() != '"') throw err("expected key");
            String k = string();
            ws();
            if (peek() != ':') throw err("expected :");
            i++;
            ws();
            m.put(k, value(depth + 1));
            ws();
            char c = peek();
            i++;
            if (c == '}') return m;
            if (c != ',') throw err("expected , or }");
        }
    }

    private List<Object> array(int depth) {
        List<Object> l = new ArrayList<Object>();
        i++;
        ws();
        if (peek() == ']') {
            i++;
            return l;
        }
        while (true) {
            ws();
            l.add(value(depth + 1));
            ws();
            char c = peek();
            i++;
            if (c == ']') return l;
            if (c != ',') throw err("expected , or ]");
        }
    }

    private char peek() {
        if (i >= s.length()) throw err("unexpected end");
        return s.charAt(i);
    }

    private String string() {
        i++;
        StringBuilder b = new StringBuilder();
        while (true) {
            if (i >= s.length()) throw err("unterminated string");
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c == '\\') {
                if (i >= s.length()) throw err("bad escape");
                char e = s.charAt(i++);
                switch (e) {
                    case '"': b.append('"'); break;
                    case '\\': b.append('\\'); break;
                    case '/': b.append('/'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'n': b.append('\n'); break;
                    case 'r': b.append('\r'); break;
                    case 't': b.append('\t'); break;
                    case 'u':
                        if (i + 4 > s.length()) throw err("bad unicode");
                        b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default: throw err("bad escape");
                }
            } else {
                b.append(c);
            }
        }
    }

    private Object number() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (start == i) throw err("unexpected character");
        try {
            return Double.valueOf(s.substring(start, i));
        } catch (NumberFormatException e) {
            throw err("bad number");
        }
    }

    public static String write(Object v) {
        StringBuilder b = new StringBuilder();
        write(b, v);
        return b.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(StringBuilder b, Object v) {
        if (v == null) {
            b.append("null");
        } else if (v instanceof String) {
            quote(b, (String) v);
        } else if (v instanceof Boolean) {
            b.append(v.toString());
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) b.append("null");
            else if (d == Math.rint(d) && Math.abs(d) < 1e15) b.append((long) d).append(".0");
            else b.append(d);
        } else if (v instanceof Number) {
            b.append(((Number) v).longValue());
        } else if (v instanceof Map) {
            b.append('{');
            Iterator<Map.Entry<String, Object>> it = ((Map<String, Object>) v).entrySet().iterator();
            boolean first = true;
            while (it.hasNext()) {
                Map.Entry<String, Object> e = it.next();
                if (!first) b.append(',');
                first = false;
                quote(b, e.getKey());
                b.append(':');
                write(b, e.getValue());
            }
            b.append('}');
        } else if (v instanceof Iterable) {
            b.append('[');
            boolean first = true;
            for (Object o : (Iterable<Object>) v) {
                if (!first) b.append(',');
                first = false;
                write(b, o);
            }
            b.append(']');
        } else {
            quote(b, v.toString());
        }
    }

    private static void quote(StringBuilder b, String s) {
        b.append('"');
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20 || c == ' ' || c == ' ') b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        b.append('"');
    }

    public static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (int k = 0; k + 1 < kv.length; k += 2) m.put((String) kv[k], kv[k + 1]);
        return m;
    }
}
