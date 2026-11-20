package dev.henny.vui.dev;

import dev.henny.vui.dom.Document;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DevTarget {
    List<Document> documents();

    Document open(String id);

    default byte[] screenshot() {
        return null;
    }

    default CompletableFuture<byte[]> screenshotAsync() {
        return CompletableFuture.completedFuture(screenshot());
    }

    default Document active() {
        List<Document> all = documents();
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }
}
