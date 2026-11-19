package dev.henny.vui.access;

public interface Setter {
    Class<?> type();

    void set(Object target, Object value) throws Throwable;
}
