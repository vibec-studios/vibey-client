package dev.henny.vui.expr;

public final class Coerce {
    private Coerce() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object to(Class<?> type, Object value) {
        if (value == Scope.MISSING) value = null;
        if (type == Object.class) return value;
        if (value != null && type.isInstance(value)) return value;
        if (type == String.class || type == CharSequence.class) return value == null ? null : Values.display(value);
        if (type == boolean.class || type == Boolean.class) return Values.truthy(value);
        if (type == int.class || type == Integer.class) return (int) Math.round(Values.toNumber(value));
        if (type == long.class || type == Long.class) return Math.round(Values.toNumber(value));
        if (type == double.class || type == Double.class) return Values.toNumber(value);
        if (type == float.class || type == Float.class) return (float) Values.toNumber(value);
        if (type == short.class || type == Short.class) return (short) Math.round(Values.toNumber(value));
        if (type == byte.class || type == Byte.class) return (byte) Math.round(Values.toNumber(value));
        if (type == char.class || type == Character.class) {
            String s = Values.display(value);
            return s.isEmpty() ? '\0' : s.charAt(0);
        }
        if (type.isEnum() && value != null) {
            for (Object constant : type.getEnumConstants()) {
                if (((Enum) constant).name().equalsIgnoreCase(Values.display(value))) return constant;
            }
        }
        if (value == null && type.isPrimitive()) return to(type, 0);
        return value;
    }

    public static boolean accepts(Class<?> type, Object value) {
        if (value == null || value == Scope.MISSING) return !type.isPrimitive();
        if (type.isInstance(value)) return true;
        if (value instanceof Number) return type.isPrimitive() || Number.class.isAssignableFrom(type) || type == String.class;
        return type == String.class || type == boolean.class || type == Boolean.class || type.isEnum();
    }
}
