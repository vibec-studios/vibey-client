package dev.henny.cosmetico;

import dev.henny.cosmetico.anim.Clock;
import dev.henny.cosmetico.config.CosmeticoConfig;
import dev.henny.cosmetico.dev.DevItemService;
import dev.henny.cosmetico.draw.CosmeticMix;
import dev.henny.cosmetico.draw.ShownBundle;
import dev.henny.cosmetico.fx.EventRunner;
import dev.henny.cosmetico.fx.FxSink;
import dev.henny.cosmetico.host.BundleResources;
import dev.henny.cosmetico.host.CosmeticoHost;
import dev.henny.cosmetico.host.MainQueue;
import dev.henny.cosmetico.load.IndexLoader;
import dev.henny.cosmetico.preview.PreviewState;
import dev.henny.cosmetico.registry.ItemRegistry;
import dev.henny.cosmetico.state.AvatarLoadouts;
import dev.henny.cosmetico.state.AvatarRegistry;
import dev.henny.cosmetico.state.EmoteControl;
import dev.henny.cosmetico.thumbs.DevThumbnails;
import dev.henny.cosmetico.version.VersionGate;

import java.util.function.Supplier;

public final class Engine {
    private final Supplier<CosmeticoHost> host;
    public final Clock clock;
    public final CosmeticoConfig config = new CosmeticoConfig();
    public final ItemRegistry items;
    public final AvatarRegistry avatars = new AvatarRegistry();
    public final AvatarLoadouts loadouts = new AvatarLoadouts(avatars);
    public final MainQueue main = new MainQueue();
    public final DevThumbnails devThumbnails = new DevThumbnails();
    public final VersionGate gate;
    public final EmoteControl emotes;
    public final ShownBundle shown;
    public final DevItemService devItems;
    private volatile BundleResources resources = BundleResources.NONE;
    private volatile FxSink backendFx = FxSink.NONE;

    public Engine(Supplier<CosmeticoHost> host, Clock clock) {
        this.host = host;
        this.clock = clock;
        this.items = new ItemRegistry(() -> host.get().log());
        this.gate = new VersionGate(() -> host.get().contentVersion());
        this.emotes = new EmoteControl(avatars, items, clock, this::resources);
        this.shown = new ShownBundle(items, config);
        this.devItems = new DevItemService(items, devThumbnails, () -> host.get().localActions(), this::resources, () -> gate);
        items.listenItems(new Lifecycle(this::resources, emotes));
    }

    public CosmeticoHost host() {
        return host.get();
    }

    public BundleResources resources() {
        return resources;
    }

    public void resources(BundleResources r) {
        resources = r == null ? BundleResources.NONE : r;
    }

    public FxSink fx() {
        FxSink own = host.get().effects();
        return own != null && own != FxSink.NONE ? own : backendFx;
    }

    public void backendFx(FxSink fx) {
        backendFx = fx == null ? FxSink.NONE : fx;
    }

    public PreviewState preview() {
        return new PreviewState(emotes, shown);
    }

    public CosmeticMix cosmeticMix() {
        return new CosmeticMix(shown);
    }

    public EventRunner eventRunner() {
        return new EventRunner(shown, config, this::fx, () -> host.get().log());
    }

    public IndexLoader indexLoader() {
        return new IndexLoader(items, gate, resources, host.get().log());
    }
}
