package dev.henny.vibeyclient.client.access;

import dev.henny.vibeyclient.access.AccessFailure;
import dev.henny.vibeyclient.access.AccessInfo;
import dev.henny.vibeyclient.access.AccessSource;
import dev.henny.vibeyclient.access.AccessStatus;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.service.VibeBackend.ApiException;
import dev.henny.vibeyclient.backend.BackendClient;

import java.util.concurrent.CompletableFuture;

final class BackendAccessSource implements AccessSource {
    private final BackendClient backend;

    BackendAccessSource(BackendClient backend) {
        this.backend = backend;
    }

    @Override
    public String accountKey() {
        return backend.prepareAccount();
    }

    @Override
    public boolean online() {
        return GlobalSettings.online();
    }

    @Override
    public CompletableFuture<AccessInfo> fetch(boolean userAction) {
        CompletableFuture<AccessInfo> out = new CompletableFuture<>();
        backend.fetchAccess(userAction).whenComplete((json, error) -> {
            ApiException e = error == null ? null : BackendClient.unwrap(error);
            if (e == null) out.complete(AccessInfo.parse(json));
            else if (e.status == 404) out.complete(AccessInfo.of(AccessStatus.APPROVED));
            else out.completeExceptionally(failure(e));
        });
        return out;
    }

    static AccessFailure failure(ApiException e) {
        if (e.status == 403 && AccessStatus.isDenial(e.code)) return new AccessFailure(AccessFailure.Kind.DENIED, e.code, e.getMessage());
        boolean transientError = "network".equals(e.code) || "mojang_failed".equals(e.code) || "rate_limited".equals(e.code)
                || e.status >= 500 || e.status == 429;
        AccessFailure.Kind kind = transientError ? AccessFailure.Kind.UNREACHABLE : AccessFailure.Kind.NO_LOGIN;
        return new AccessFailure(kind, e.code, e.getMessage());
    }
}
