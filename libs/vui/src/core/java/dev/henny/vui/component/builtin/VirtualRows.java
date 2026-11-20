package dev.henny.vui.component.builtin;

import dev.henny.vui.dom.ChildSource;
import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.build.ForItems;
import dev.henny.vui.dom.build.SlotContent;
import dev.henny.vui.dom.build.TreeBuilder;
import dev.henny.vui.expr.VarScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VirtualRows implements ChildSource {
    private final Element list;
    private final SlotContent content;
    private final TreeBuilder builder;
    private final Map<Integer, Row> rows = new HashMap<>();
    private final List<Row> pool = new ArrayList<>();
    private List<ForItems.Item> items = List.of();
    private int first = -1;
    private int last = -1;
    private boolean stale = true;

    VirtualRows(Element list, SlotContent content, TreeBuilder builder) {
        this.list = list;
        this.content = content;
        this.builder = builder;
    }

    public int count() {
        refreshItems();
        return items.size();
    }

    public int indexOf(Element child) {
        for (Row r : rows.values()) if (r.element == child) return r.index;
        return -1;
    }

    public int built() {
        return rows.size();
    }

    void itemsChanged() {
        stale = true;
        first = -1;
        last = -1;
    }

    void update() {
        refreshItems();
        float size = ListBehavior.itemSize(list);
        float viewport = list.box().innerHeight() > 0 ? list.box().innerHeight() : size * 10;
        int from = Math.max(0, (int) Math.floor(list.scrollY() / size) - ListBehavior.OVERSCAN);
        int to = Math.min(items.size() - 1, (int) Math.ceil((list.scrollY() + viewport) / size) + ListBehavior.OVERSCAN);
        if (from == first && to == last) return;
        first = from;
        last = to;
        rows.entrySet().removeIf(entry -> {
            int i = entry.getKey();
            if (i >= from && i <= to) return false;
            pool.add(entry.getValue());
            return true;
        });
        for (int i = from; i <= to; i++) {
            Row row = rows.get(i);
            if (row == null) {
                row = pool.isEmpty() ? create() : pool.remove(pool.size() - 1);
                rows.put(i, row);
            }
            row.bind(i, items.get(i));
        }
        list.rebuildChildren();
        list.markLayoutDirty();
    }

    private void refreshItems() {
        if (!stale) return;
        stale = false;
        items = ForItems.of(list.prop("items"));
    }

    private Row create() {
        String[] names = names();
        VarScope scope = new VarScope(content.scope(), names, new Object[names.length]);
        List<ChildSource> sources = new ArrayList<>();
        builder.buildChildren(list, content.nodes(), scope, content.context(), sources);
        List<Element> built = new ArrayList<>();
        for (ChildSource s : sources) s.collect(built);
        Element element = built.isEmpty() ? new Element("box", builder.host().behavior("box"), list.pos()) : built.get(0);
        return new Row(scope, sources, element, names);
    }

    private String[] names() {
        if (content.spec() != null && content.spec().destructure().length > 0) return content.spec().destructure();
        if (content.spec() != null && content.spec().variable() != null) return new String[]{content.spec().variable()};
        return new String[]{"item", "index"};
    }

    @Override
    public void collect(List<Element> out) {
        for (int i = first; i <= last && i >= 0; i++) {
            Row r = rows.get(i);
            if (r != null) out.add(r.element);
        }
    }

    @Override
    public void dispose() {
        for (Row r : rows.values()) r.dispose();
        for (Row r : pool) r.dispose();
        rows.clear();
        pool.clear();
    }

    private static final class Row {
        final VarScope scope;
        final List<ChildSource> sources;
        final Element element;
        final String[] names;
        int index;

        Row(VarScope scope, List<ChildSource> sources, Element element, String[] names) {
            this.scope = scope;
            this.sources = sources;
            this.element = element;
            this.names = names;
        }

        void bind(int i, ForItems.Item item) {
            index = i;
            for (int n = 0; n < names.length; n++) {
                String name = names[n];
                if (name.equals("index")) scope.set(n, (double) i);
                else if (n == 0 || name.equals("item")) scope.set(n, item.value());
            }
        }

        void dispose() {
            for (ChildSource s : sources) s.dispose();
        }
    }
}
