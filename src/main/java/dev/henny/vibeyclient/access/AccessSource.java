package dev.henny.vibeyclient.access;

import java.util.concurrent.CompletableFuture;

public interface AccessSource {
    String accountKey();

    boolean online();

    CompletableFuture<AccessInfo> fetch(boolean userAction);
}
