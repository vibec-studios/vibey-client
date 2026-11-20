package dev.henny.companio.parse;

import dev.henny.companio.expr.Names;
import dev.henny.companio.sense.Variables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CompanionNames implements Names {
    private final Set<String> known = new HashSet<>(Variables.BUILTIN);
    private final List<String> all = new ArrayList<>(Variables.BUILTIN);

    public CompanionNames(Collection<String> needs, Collection<String> vars) {
        for (String n : needs) add("need." + n);
        for (String v : vars) add("var." + v);
    }

    private void add(String name) {
        if (known.add(name)) all.add(name);
    }

    public boolean known(String variable) {
        return known.contains(variable);
    }

    public Collection<String> all() {
        return all;
    }
}
