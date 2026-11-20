package dev.henny.companio.expr;

import java.util.Collection;

public interface Names {
    boolean known(String variable);

    Collection<String> all();
}
