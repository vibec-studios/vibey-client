package dev.henny.vui.access;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassAccess {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodType GETTER_TYPE = MethodType.methodType(Object.class, Object.class);
    private static final MethodType SETTER_TYPE = MethodType.methodType(void.class, Object.class, Object.class);
    private static final MethodType SPREAD_TYPE = MethodType.methodType(Object.class, Object.class, Object[].class);

    private final Class<?> type;
    private final ConcurrentHashMap<String, Optional<Getter>> getters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Optional<Setter>> setters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Optional<Invoker>> invokers = new ConcurrentHashMap<>();

    ClassAccess(Class<?> type) {
        this.type = type;
    }

    public Getter getter(String name) {
        return getters.computeIfAbsent(name, n -> Optional.ofNullable(findGetter(n))).orElse(null);
    }

    public Setter setter(String name) {
        return setters.computeIfAbsent(name, n -> Optional.ofNullable(findSetter(n))).orElse(null);
    }

    public Invoker invoker(String name, int arity) {
        return invokers.computeIfAbsent(name + "/" + arity, k -> Optional.ofNullable(findInvoker(name, arity))).orElse(null);
    }

    public boolean hasMethod(String name) {
        for (Method m : MemberSearch.methods(type)) if (m.getName().equals(name)) return true;
        return false;
    }

    private Getter findGetter(String name) {
        String cap = capitalize(name);
        for (String candidate : new String[]{"get" + cap, "is" + cap, name}) {
            Method m = MemberSearch.method(type, candidate, 0);
            if (m != null && m.getReturnType() != void.class) {
                MethodHandle h = handle(m, GETTER_TYPE);
                if (h != null) return h::invokeExact;
            }
        }
        Field f = MemberSearch.field(type, name);
        if (f == null) return null;
        try {
            MethodHandle h = LOOKUP.unreflectGetter(f).asType(GETTER_TYPE);
            return h::invokeExact;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private Setter findSetter(String name) {
        Method m = MemberSearch.method(type, "set" + capitalize(name), 1);
        if (m != null) {
            MethodHandle h = handle(m, SETTER_TYPE);
            if (h != null) return new HandleSetter(h, m.getParameterTypes()[0]);
        }
        Field f = MemberSearch.field(type, name);
        if (f == null || java.lang.reflect.Modifier.isFinal(f.getModifiers())) return null;
        try {
            return new HandleSetter(LOOKUP.unreflectSetter(f).asType(SETTER_TYPE), f.getType());
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private Invoker findInvoker(String name, int arity) {
        Method m = MemberSearch.method(type, name, arity);
        if (m == null) return null;
        try {
            MethodHandle h = LOOKUP.unreflect(m);
            MethodHandle spread = h.asSpreader(Object[].class, arity).asType(SPREAD_TYPE);
            return new Invoker(spread, m.getParameterTypes());
        } catch (IllegalAccessException | IllegalArgumentException e) {
            return null;
        }
    }

    private static MethodHandle handle(Method m, MethodType type) {
        try {
            return LOOKUP.unreflect(m).asType(type);
        } catch (IllegalAccessException | IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private record HandleSetter(MethodHandle handle, Class<?> type) implements Setter {
        @Override
        public void set(Object target, Object value) throws Throwable {
            handle.invokeExact(target, value);
        }
    }
}
