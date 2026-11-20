package dev.henny.vibeyclient.gametest.parity;

import java.lang.reflect.Field;

final class StaticFields {
    private StaticFields() {}

    static Object get(Class<?> owner, String name) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(owner.getSimpleName() + "." + name, e);
        }
    }

    static void set(Class<?> owner, String name, Object value) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            f.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(owner.getSimpleName() + "." + name, e);
        }
    }
}
