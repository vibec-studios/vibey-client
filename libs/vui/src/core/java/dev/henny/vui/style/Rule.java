package dev.henny.vui.style;

import dev.henny.vui.diag.SourcePos;

import java.util.List;

public record Rule(Selector selector, List<Declaration> declarations, int order, SourcePos pos) {

    public int specificity() {
        return selector.specificity();
    }

    public boolean outranks(Rule other) {
        if (specificity() != other.specificity()) return specificity() > other.specificity();
        return order > other.order;
    }
}
