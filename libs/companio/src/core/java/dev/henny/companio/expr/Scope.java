package dev.henny.companio.expr;

public interface Scope {
    Object variable(String name);

    Object call(String function, Object[] args);
}
