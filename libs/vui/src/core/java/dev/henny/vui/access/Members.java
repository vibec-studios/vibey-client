package dev.henny.vui.access;

import dev.henny.vui.bind.Action;
import dev.henny.vui.bind.Model;
import dev.henny.vui.bind.ReadableValue;
import dev.henny.vui.bind.State;
import dev.henny.vui.bind.Tracker;
import dev.henny.vui.expr.Coerce;
import dev.henny.vui.expr.EvalException;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.Values;

import java.util.List;
import java.util.Map;

public final class Members {
    private Members() {
    }

    public static Object get(Object target, String name) {
        if (target == null || target == Scope.MISSING) {
            throw new EvalException("cannot read '" + name + "' of " + Values.typeName(target), "check that the value exists, e.g. a && a." + name);
        }
        if (target instanceof Model m) return m.has(name) ? m.get(name) : Scope.MISSING;
        if (target instanceof Map<?, ?> map) {
            Tracker.untrackedRead();
            return map.containsKey(name) ? Values.unwrap(map.get(name)) : Scope.MISSING;
        }
        if ((name.equals("length") || name.equals("size")) && isSized(target)) return (double) Values.length(target);
        Getter getter = Accessors.of(target.getClass()).getter(name);
        if (getter == null) return Scope.MISSING;
        Object value = invokeGetter(getter, target, name);
        if (value instanceof ReadableValue<?> r) return r.get();
        Tracker.untrackedRead();
        return value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean set(Object target, String name, Object value) {
        if (target == null || target == Scope.MISSING) return false;
        if (target instanceof Model m) {
            m.value(name, value);
            return true;
        }
        if (target instanceof Map map) {
            map.put(name, value);
            return true;
        }
        ClassAccess access = Accessors.of(target.getClass());
        Getter getter = access.getter(name);
        if (getter != null) {
            Object holder = Tracker.untracked(() -> invokeGetter(getter, target, name));
            if (holder instanceof State s) {
                s.set(value);
                return true;
            }
        }
        Setter setter = access.setter(name);
        if (setter == null) return false;
        try {
            setter.set(target, Coerce.to(setter.type(), value));
            return true;
        } catch (Throwable t) {
            throw new EvalException("could not write '" + name + "': " + t, null);
        }
    }

    public static Object call(Object target, String name, Object[] args) {
        if (target instanceof Model m) {
            Object entry = m.entry(name);
            if (entry instanceof Action a) return a.invoke(args);
            return Scope.MISSING;
        }
        if (target == null) return Scope.MISSING;
        Invoker invoker = Accessors.of(target.getClass()).invoker(name, args.length);
        if (invoker == null) return Scope.MISSING;
        try {
            return invoker.invoke(target, args);
        } catch (EvalException e) {
            throw e;
        } catch (Throwable t) {
            throw new EvalException(name + "(…) threw " + t, "the exception came from your Java code");
        }
    }

    private static boolean isSized(Object target) {
        return target instanceof CharSequence || target instanceof List<?> || target instanceof Object[]
                || target instanceof java.util.Collection<?>;
    }

    private static Object invokeGetter(Getter getter, Object target, String name) {
        try {
            return getter.get(target);
        } catch (Throwable t) {
            throw new EvalException("reading '" + name + "' threw " + t, "the exception came from your Java code");
        }
    }
}
