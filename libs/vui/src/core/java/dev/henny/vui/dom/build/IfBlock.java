package dev.henny.vui.dom.build;

import dev.henny.vui.bind.Effect;
import dev.henny.vui.bind.Tracker;
import dev.henny.vui.dom.ChildSource;
import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.Values;
import dev.henny.vui.template.CElement;

import java.util.ArrayList;
import java.util.List;

public final class IfBlock implements ChildSource {
    private final TreeBuilder builder;
    private final Element parent;
    private final List<CElement> branches;
    private final Scope scope;
    private final BuildContext ctx;
    private final Effect effect;
    private final List<ChildSource> current = new ArrayList<>();
    private final List<Element> leaving = new ArrayList<>();
    private int active = -2;
    private boolean ready;

    IfBlock(TreeBuilder builder, Element parent, List<CElement> branches, Scope scope, BuildContext ctx) {
        this.builder = builder;
        this.parent = parent;
        this.branches = branches;
        this.scope = scope;
        this.ctx = ctx;
        this.effect = builder.host().effect(this::evaluate);
        effect.run();
        ready = true;
    }

    public int activeBranch() {
        return active;
    }

    private void evaluate() {
        int next = -1;
        for (int i = 0; i < branches.size(); i++) {
            CElement b = branches.get(i);
            if (b.condition() == null || Values.truthy(b.condition().evaluate(scope, builder.host().errors()))) {
                next = i;
                break;
            }
        }
        if (next == active) return;
        int chosen = next;
        Tracker.untracked(() -> {
            swap(chosen);
            return null;
        });
    }

    private void swap(int next) {
        for (Element old : Leave.collectAll(current)) {
            if (Leave.begin(old, this)) leaving.add(old);
        }
        current.clear();
        active = next;
        if (next >= 0) builder.buildChildren(parent, List.of(branches.get(next).withoutBranch()), scope, ctx, current);
        if (ready) parent.rebuildChildren();
    }

    @Override
    public void collect(List<Element> out) {
        for (ChildSource s : current) s.collect(out);
        out.addAll(leaving);
    }

    @Override
    public void removeLeft(Element element) {
        if (leaving.remove(element)) parent.rebuildChildren();
    }

    @Override
    public void dispose() {
        effect.dispose();
        for (ChildSource s : current) s.dispose();
        current.clear();
        for (Element e : leaving) dev.henny.vui.dom.Lifecycle.dispose(e);
        leaving.clear();
    }
}
