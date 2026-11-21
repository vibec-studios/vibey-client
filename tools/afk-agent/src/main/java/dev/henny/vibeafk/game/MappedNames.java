package dev.henny.vibeafk.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class MappedNames implements Names {
    private final ProGuard pg;
    private final Tiny tiny;

    public MappedNames(ProGuard pg, Tiny tiny) {
        this.pg = pg;
        this.tiny = tiny;
    }

    @Override
    public String kind() {
        return tiny == null ? "obf" : "intermediary";
    }

    private String obfInternal(String moj) {
        String o = pg.mojToObf.get(moj);
        return o == null ? null : o.replace('.', '/');
    }

    private String runtimeInternalFromObf(String obfInternal) {
        if (tiny == null) return obfInternal;
        String r = tiny.classes.get(obfInternal);
        return r == null ? obfInternal : r;
    }

    @Override
    public String cls(String moj) {
        String o = obfInternal(moj);
        return o == null ? null : runtimeInternalFromObf(o).replace('/', '.');
    }

    @Override
    public String mojOf(String runtime) {
        String internal = runtime.replace('.', '/');
        String obf = internal;
        if (tiny != null) {
            String back = tiny.reverse.get(internal);
            if (back != null) obf = back;
        }
        return pg.obfToMoj.get(obf.replace('/', '.'));
    }

    @Override
    public String field(String mojOwner, String mojField) {
        Map<String, ProGuard.Field> m = pg.fields.get(mojOwner);
        ProGuard.Field f = m == null ? null : m.get(mojField);
        if (f == null) return null;
        if (tiny == null) return f.obf;
        String owner = obfInternal(mojOwner);
        String r = tiny.members.get(Tiny.key(owner, f.obf, officialDesc(f.type)));
        return r == null ? f.obf : r;
    }

    @Override
    public List<Member> methods(String mojOwner, String mojName) {
        List<ProGuard.Method> l = pg.methods.get(mojOwner);
        if (l == null) return Collections.emptyList();
        List<Member> out = new ArrayList<Member>();
        String owner = obfInternal(mojOwner);
        for (ProGuard.Method m : l) {
            if (!m.name.equals(mojName)) continue;
            String name = m.obf;
            if (tiny != null) {
                StringBuilder d = new StringBuilder("(");
                for (String p : m.params) d.append(officialDesc(p));
                d.append(')').append(officialDesc(m.ret));
                String r = tiny.members.get(Tiny.key(owner, m.obf, d.toString()));
                if (r != null) name = r;
            }
            String[] params = new String[m.params.length];
            for (int k = 0; k < params.length; k++) params[k] = runtimeTypeName(m.params[k]);
            out.add(new Member(name, params));
        }
        return out;
    }

    String officialDesc(String javaType) {
        int dims = 0;
        String t = javaType.trim();
        while (t.endsWith("[]")) {
            dims++;
            t = t.substring(0, t.length() - 2);
        }
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < dims; k++) b.append('[');
        String prim = TypeNames.primitiveDesc(t);
        if (prim != null) return b.append(prim).toString();
        String o = obfInternal(t);
        return b.append('L').append(o == null ? t.replace('.', '/') : o).append(';').toString();
    }

    String runtimeTypeName(String javaType) {
        int dims = 0;
        String t = javaType.trim();
        while (t.endsWith("[]")) {
            dims++;
            t = t.substring(0, t.length() - 2);
        }
        String base;
        String prim = TypeNames.primitiveDesc(t);
        if (prim != null) {
            if (dims == 0) return t;
            base = prim;
        } else {
            String r = cls(t);
            String name = r == null ? t : r;
            if (dims == 0) return name;
            base = "L" + name + ";";
        }
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < dims; k++) b.append('[');
        return b.append(base).toString();
    }
}
