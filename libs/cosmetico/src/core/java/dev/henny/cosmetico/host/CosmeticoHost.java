package dev.henny.cosmetico.host;

import dev.henny.cosmetico.fx.FxSink;

import java.util.concurrent.Executor;

public interface CosmeticoHost {
    String language();

    String contentVersion();

    default Log log() {
        return Log.SYSTEM;
    }

    default Executor workers() {
        return Workers.shared();
    }

    default FxSink effects() {
        return FxSink.NONE;
    }

    default LocalActions localActions() {
        return LocalActions.NONE;
    }

    default ThumbnailFetcher thumbnails() {
        return ThumbnailFetcher.NONE;
    }

    CosmeticoHost DEFAULT = new DefaultHost();
}
