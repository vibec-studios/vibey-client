package dev.henny.vui.access;

@FunctionalInterface
public interface Getter {
    Object get(Object target) throws Throwable;
}
