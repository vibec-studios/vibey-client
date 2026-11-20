package dev.henny.vui.access;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

final class MemberSearch {
    private MemberSearch() {
    }

    static List<Method> methods(Class<?> type) {
        List<Method> out = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) if (!Modifier.isStatic(m.getModifiers()) && !m.isSynthetic()) out.add(m);
        }
        return out;
    }

    static Method method(Class<?> type, String name, int arity) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name) || m.getParameterCount() != arity) continue;
                if (Modifier.isStatic(m.getModifiers()) || m.isSynthetic() || m.isBridge()) continue;
                Method usable = usable(type, m);
                if (usable != null) return usable;
            }
        }
        return publicInterfaceMethod(type, name, arity);
    }

    static Field field(Class<?> type, String name) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                if (Modifier.isStatic(f.getModifiers())) return null;
                if (makeAccessible(f)) return f;
                return null;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Method usable(Class<?> type, Method m) {
        if (Modifier.isPublic(m.getModifiers()) && Modifier.isPublic(m.getDeclaringClass().getModifiers())) return m;
        if (makeAccessible(m)) return m;
        return publicInterfaceMethod(type, m.getName(), m.getParameterCount());
    }

    private static Method publicInterfaceMethod(Class<?> type, String name, int arity) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                Method m = interfaceMethod(i, name, arity);
                if (m != null) return m;
            }
        }
        return null;
    }

    private static Method interfaceMethod(Class<?> iface, String name, int arity) {
        if (Modifier.isPublic(iface.getModifiers())) {
            for (Method m : iface.getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == arity && !Modifier.isStatic(m.getModifiers())) return m;
            }
        }
        for (Class<?> parent : iface.getInterfaces()) {
            Method m = interfaceMethod(parent, name, arity);
            if (m != null) return m;
        }
        return null;
    }

    private static boolean makeAccessible(java.lang.reflect.AccessibleObject member) {
        try {
            member.setAccessible(true);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
