package dev.henny.vui.access;

import dev.henny.vui.expr.Coerce;

import java.lang.invoke.MethodHandle;

public final class Invoker {
    private final MethodHandle spread;
    private final Class<?>[] parameterTypes;

    Invoker(MethodHandle spread, Class<?>[] parameterTypes) {
        this.spread = spread;
        this.parameterTypes = parameterTypes;
    }

    public int arity() {
        return parameterTypes.length;
    }

    public Object invoke(Object target, Object[] args) throws Throwable {
        Object[] converted = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            converted[i] = Coerce.to(parameterTypes[i], i < args.length ? args[i] : null);
        }
        return (Object) spread.invokeExact(target, converted);
    }
}
