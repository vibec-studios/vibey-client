package dev.henny.vui.template;

import dev.henny.vui.diag.Diagnostics;

import java.util.List;

final class BranchCheck {
    private BranchCheck() {
    }

    static void check(List<CNode> siblings, Diagnostics diagnostics) {
        boolean open = false;
        for (CNode n : siblings) {
            if (!(n instanceof CElement e)) {
                open = false;
                continue;
            }
            switch (e.branch()) {
                case IF -> open = true;
                case ELSE_IF -> {
                    if (!open) diagnostics.error(e.pos(), "v-else-if without a v-if before it", "put it right after an element with v-if");
                }
                case ELSE -> {
                    if (!open) diagnostics.error(e.pos(), "v-else without a v-if before it", "put it right after an element with v-if or v-else-if");
                    open = false;
                }
                case NONE -> open = false;
            }
            if (e.forSpec() != null && e.branch() != Branch.NONE) {
                diagnostics.warn(e.pos(), "v-if and v-for on the same element", "wrap it in <template v-for=…> and put v-if inside");
            }
        }
    }
}
