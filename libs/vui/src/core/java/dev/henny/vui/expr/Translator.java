package dev.henny.vui.expr;

@FunctionalInterface
public interface Translator {
    Translator KEYS = (key, args) -> key;

    String translate(String key, Object[] args);
}
