package dev.henny.vui.dom;

import dev.henny.vui.bind.Effect;

public final class Lifecycle {
    private Lifecycle() {
    }

    public static void mount(Element e) {
        if (e.flags().mounted) return;
        e.flags().mounted = true;
        Document doc = e.document();
        if (doc != null && doc.isMounted() && !e.flags().internal) {
            e.flags().entering = true;
            doc.hooks().entering(e);
        }
        if (e.behavior() != null) e.behavior().mount(e);
        if (doc != null) doc.hooks().registerMounted(e);
        for (int i = 0; i < e.childCount(); i++) mountQuietly(e.child(i));
    }

    private static void mountQuietly(Element e) {
        if (e.flags().mounted) return;
        e.flags().mounted = true;
        if (e.behavior() != null) e.behavior().mount(e);
        if (e.document() != null) e.document().hooks().registerMounted(e);
        for (int i = 0; i < e.childCount(); i++) mountQuietly(e.child(i));
    }

    public static void dispose(Element e) {
        if (e.flags().disposed) return;
        e.flags().disposed = true;
        for (Effect effect : e.effects()) effect.dispose();
        e.effects().clear();
        for (ChildSource s : e.sources()) s.dispose();
        if (e.behavior() != null && e.flags().mounted) e.behavior().unmount(e);
        if (e.document() != null) e.document().hooks().forget(e);
    }
}
