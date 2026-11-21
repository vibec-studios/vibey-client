package dev.henny.vibeyclient.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

final class FakeAccessSource implements AccessSource {
    String key = "https://vibe.test|player";
    boolean online = true;
    final List<CompletableFuture<AccessInfo>> calls = new ArrayList<>();
    final List<Boolean> userActions = new ArrayList<>();

    @Override
    public String accountKey() {
        return key;
    }

    @Override
    public boolean online() {
        return online;
    }

    @Override
    public synchronized CompletableFuture<AccessInfo> fetch(boolean userAction) {
        CompletableFuture<AccessInfo> f = new CompletableFuture<>();
        calls.add(f);
        userActions.add(userAction);
        return f;
    }

    CompletableFuture<AccessInfo> last() {
        return calls.get(calls.size() - 1);
    }

    void answer(AccessInfo info) {
        last().complete(info);
    }

    void fail(AccessFailure.Kind kind) {
        last().completeExceptionally(new AccessFailure(kind, kind.name().toLowerCase(Locale.ROOT), "fake"));
    }
}
