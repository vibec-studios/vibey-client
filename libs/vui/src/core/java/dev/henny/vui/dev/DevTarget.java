package dev.henny.vui.dev;

import dev.henny.vui.dom.Document;

import java.util.List;

public interface DevTarget {
    List<Document> documents();

    Document open(String id);

    byte[] screenshot();

    default Document active() {
        List<Document> all = documents();
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }
}
