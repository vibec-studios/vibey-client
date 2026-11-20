package dev.henny.vui.dom.build;

import dev.henny.vui.bind.Effect;
import dev.henny.vui.bind.Tracker;
import dev.henny.vui.dom.ChildSource;
import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.VarScope;
import dev.henny.vui.template.CElement;
import dev.henny.vui.template.ForSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ForBlock implements ChildSource {
    private final TreeBuilder builder;
    private final Element parent;
    private final CElement template;
    private final ForSpec spec;
    private final Scope scope;
    private final BuildContext ctx;
    private final Effect effect;
    private final String[] names;
    private Map<Object, ForEntry> entries = new HashMap<>();
    private List<ForEntry> order = new ArrayList<>();
    private final List<Element> leaving = new ArrayList<>();
    private boolean ready;

    ForBlock(TreeBuilder builder, Element parent, CElement node, Scope scope, BuildContext ctx) {
        this.builder = builder;
        this.parent = parent;
        this.template = node.withoutFor();
        this.spec = node.forSpec();
        this.scope = scope;
        this.ctx = ctx;
        this.names = spec.names();
        this.effect = builder.host().effect(this::evaluate);
        effect.run();
        ready = true;
    }

    public int size() {
        return order.size();
    }

    private void evaluate() {
        List<ForItems.Item> items = ForItems.of(spec.source().evaluate(scope, builder.host().errors()));
        List<Object> keys = new ArrayList<>(items.size());
        java.util.Set<Object> seen = new java.util.HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            Object key = keyOf(items.get(i), i, seen);
            seen.add(key);
            keys.add(key);
        }
        Tracker.untracked(() -> {
            reconcile(items, keys);
            return null;
        });
    }

    private Object keyOf(ForItems.Item item, int index, java.util.Set<Object> seen) {
        Object key = item.value();
        if (template.key() != null) key = template.key().evaluate(new VarScope(scope, names, values(item, index)), builder.host().errors());
        if (key == null || seen.contains(key)) key = new IndexKey(index, key);
        return key;
    }

    private Object[] values(ForItems.Item item, int index) {
        if (names.length == 1) return new Object[]{item.value()};
        if (names.length == 2) return new Object[]{item.value(), item.key() != null ? item.key() : (double) index};
        return new Object[]{item.value(), item.key(), (double) index};
    }

    private void reconcile(List<ForItems.Item> items, List<Object> keys) {
        Map<Object, ForEntry> next = new HashMap<>();
        List<ForEntry> nextOrder = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            Object key = keys.get(i);
            ForEntry entry = entries.remove(key);
            Object[] values = values(items.get(i), i);
            if (entry == null) entry = create(values);
            else for (int v = 0; v < values.length; v++) entry.scope().set(v, values[v]);
            next.put(key, entry);
            nextOrder.add(entry);
        }
        for (ForEntry gone : entries.values()) {
            for (Element e : Leave.collectAll(gone.sources())) if (Leave.begin(e, this)) leaving.add(e);
        }
        entries = next;
        order = nextOrder;
        if (ready) parent.rebuildChildren();
    }

    private ForEntry create(Object[] values) {
        VarScope itemScope = new VarScope(scope, names, values);
        List<ChildSource> sources = new ArrayList<>();
        builder.buildChildren(parent, List.of(template), itemScope, ctx, sources);
        return new ForEntry(itemScope, sources);
    }

    @Override
    public void collect(List<Element> out) {
        for (ForEntry entry : order) for (ChildSource s : entry.sources()) s.collect(out);
        out.addAll(leaving);
    }

    @Override
    public void removeLeft(Element element) {
        if (leaving.remove(element)) parent.rebuildChildren();
    }

    @Override
    public void dispose() {
        effect.dispose();
        for (ForEntry entry : order) for (ChildSource s : entry.sources()) s.dispose();
        order.clear();
        entries.clear();
        for (Element e : leaving) dev.henny.vui.dom.Lifecycle.dispose(e);
        leaving.clear();
    }

    private record IndexKey(int index, Object key) {
    }

    private record ForEntry(VarScope scope, List<ChildSource> sources) {
    }
}
