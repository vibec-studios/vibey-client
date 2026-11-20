package dev.henny.vui.access;

public final class Accessors {
    private static final ClassValue<ClassAccess> CACHE = new ClassValue<>() {
        @Override
        protected ClassAccess computeValue(Class<?> type) {
            return new ClassAccess(type);
        }
    };

    private Accessors() {
    }

    public static ClassAccess of(Class<?> type) {
        return CACHE.get(type);
    }
}
