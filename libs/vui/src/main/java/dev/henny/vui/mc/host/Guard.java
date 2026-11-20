package dev.henny.vui.mc.host;

import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.dom.Document;
import dev.henny.vui.mc.VuiMod;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

public final class Guard {
    private static final Set<String> LOGGED = new HashSet<>();

    private Guard() {
    }

    public static void run(Document doc, Runnable task) {
        try {
            task.run();
        } catch (RuntimeException | LinkageError e) {
            report(doc, e);
        }
    }

    public static boolean test(Document doc, BooleanSupplier task) {
        try {
            return task.getAsBoolean();
        } catch (RuntimeException | LinkageError e) {
            report(doc, e);
            return false;
        }
    }

    public static void report(Document doc, Throwable e) {
        String where = doc == null ? "vui" : String.valueOf(doc.documentId());
        if (LOGGED.add(where + "|" + e)) VuiMod.LOG.error("vui: {} failed", where, e);
        if (doc == null) return;
        String file = doc.template() == null ? where : doc.template().fileName();
        doc.errors().report(new SourcePos(file, 1, 1), String.valueOf(e), "Java exception, see the log");
    }
}
