package dev.henny.cosmetico.fixtures;

import dev.henny.cosmetico.Engine;
import dev.henny.cosmetico.fx.FxSink;
import dev.henny.cosmetico.host.CosmeticoHost;
import dev.henny.cosmetico.host.LocalActions;
import dev.henny.cosmetico.host.Log;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemSource;

public final class TestEngine implements CosmeticoHost {
    public final FakeClock time = new FakeClock();
    public final RecordingFx fx = new RecordingFx();
    public final RecordingActions actions = new RecordingActions();
    public final Engine engine = new Engine(() -> this, time.clock());

    @Override
    public String language() {
        return "en";
    }

    @Override
    public String contentVersion() {
        return "0.3.1";
    }

    @Override
    public Log log() {
        return Log.NONE;
    }

    @Override
    public FxSink effects() {
        return fx;
    }

    @Override
    public LocalActions localActions() {
        return actions;
    }

    public Item add(Bundle b) {
        Item i = Item.of(b, ItemSource.BUILTIN, 0, null, "default", engine.items.nextRevision());
        engine.items.put(i);
        return i;
    }
}
