package dev.henny.vibeyclient.module.cosmetics.host;

import dev.henny.cosmetico.host.CosmeticoHost;
import dev.henny.cosmetico.host.LocalActions;
import dev.henny.cosmetico.host.Log;
import dev.henny.cosmetico.host.ThumbnailFetcher;
import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.cosmetics.catalog.CatalogThumbnails;

public final class VibeyCosmeticoHost implements CosmeticoHost {
    private final Log log = new VibeyLog();
    private final LocalActions actions = new WardrobeActions();

    @Override
    public String language() {
        return Lang.german() ? "de" : "en";
    }

    @Override
    public String contentVersion() {
        return BuildInfo.VERSION;
    }

    @Override
    public Log log() {
        return log;
    }

    @Override
    public LocalActions localActions() {
        return actions;
    }

    @Override
    public ThumbnailFetcher thumbnails() {
        return CatalogThumbnails.INSTANCE;
    }
}
