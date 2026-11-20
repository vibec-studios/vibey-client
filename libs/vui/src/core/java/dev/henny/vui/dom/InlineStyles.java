package dev.henny.vui.dom;

import dev.henny.vui.style.Declaration;

import java.util.ArrayList;
import java.util.List;

final class InlineStyles {
    private List<Declaration> fixed = List.of();
    private List<Declaration> bound = List.of();
    private List<Declaration> merged = List.of();

    List<Declaration> merged() {
        return merged;
    }

    void fixed(List<Declaration> declarations) {
        fixed = declarations;
        merge();
    }

    void bound(List<Declaration> declarations) {
        bound = declarations;
        merge();
    }

    private void merge() {
        if (bound.isEmpty()) merged = fixed;
        else if (fixed.isEmpty()) merged = bound;
        else {
            List<Declaration> all = new ArrayList<>(fixed);
            all.addAll(bound);
            merged = all;
        }
    }
}
