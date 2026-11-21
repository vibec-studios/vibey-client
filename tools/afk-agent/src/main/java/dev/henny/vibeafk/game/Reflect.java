package dev.henny.vibeafk.game;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class Reflect {
    private final Names names;
    private final Opener opener;
    private final ClassLoader loader;

    public Reflect(Names names, Opener opener, ClassLoader loader) {
        this.names = names;
        this.opener = opener;
        this.loader = loader;
    }

    public Names names() {
        return names;
    }

    public Class<?> cls(String moj) {
        String r = names.cls(moj);
        if (r == null) return null;
        try {
            Class<?> c = Class.forName(r, false, loader);
            if (opener != null) opener.open(c);
            return c;
        } catch (Throwable t) {
            return null;
        }
    }

    public String mojOf(Class<?> c) {
        return c == null ? null : names.mojOf(c.getName());
    }

    public Method method(Class<?> start, String mojName, int argc) {
        for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
            String moj = names.mojOf(c.getName());
            if (moj == null) continue;
            List<Names.Member> members = names.methods(moj, mojName);
            for (Names.Member m : members) {
                Method found = declared(c, m, argc);
                if (found != null) return found;
            }
        }
        return null;
    }

    public Field field(Class<?> start, String mojName) {
        for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
            String moj = names.mojOf(c.getName());
            if (moj == null) continue;
            String r = names.field(moj, mojName);
            if (r == null) continue;
            try {
                Field f = c.getDeclaredField(r);
                if (opener != null) opener.open(c);
                f.setAccessible(true);
                return f;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private Method declared(Class<?> c, Names.Member m, int argc) {
        Method[] all;
        try {
            all = c.getDeclaredMethods();
        } catch (Throwable t) {
            return null;
        }
        for (Method x : all) {
            if (!x.getName().equals(m.runtimeName)) continue;
            Class<?>[] p = x.getParameterTypes();
            if (argc >= 0 && p.length != argc) continue;
            if (m.runtimeParams != null) {
                if (p.length != m.runtimeParams.length) continue;
                boolean ok = true;
                for (int k = 0; k < p.length && ok; k++) ok = p[k].getName().equals(m.runtimeParams[k]);
                if (!ok) continue;
            }
            try {
                if (opener != null) opener.open(c);
                x.setAccessible(true);
                return x;
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    public static Object get(Field f, Object target) {
        if (f == null || (target == null && !java.lang.reflect.Modifier.isStatic(f.getModifiers()))) return null;
        try {
            return f.get(target);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object call(Method m, Object target, Object... args) {
        if (m == null) return null;
        try {
            return m.invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }

    public String text(Object component) {
        if (component == null) return null;
        if (opener != null) opener.open(component.getClass());
        try {
            Method m = component.getClass().getMethod("getString");
            m.setAccessible(true);
            Object r = m.invoke(component);
            return r == null ? null : r.toString();
        } catch (Throwable t) {
            return null;
        }
    }
}
