package dev.henny.companio.mc.client;

import dev.henny.companio.config.CompanioConfig;
import dev.henny.companio.mc.fx.AnimEvents;
import dev.henny.companio.mc.fx.McOutput;
import dev.henny.companio.mc.interact.Bubbles;
import dev.henny.companio.mc.interact.CompanionTarget;
import dev.henny.companio.mc.interact.Interactions;
import dev.henny.companio.mc.packs.PackRepository;
import dev.henny.companio.mc.render.CompanionRenderer;
import dev.henny.companio.mc.screen.Overlay;
import dev.henny.companio.mc.world.McBlockView;
import dev.henny.companio.mc.world.McSenses;
import dev.henny.companio.pack.Pack;
import dev.henny.companio.pack.PackRegistry;
import dev.henny.companio.sense.Senses;
import dev.henny.companio.sync.Puppet;
import dev.henny.companio.sync.RemoteCompanions;
import dev.henny.companio.sync.SyncPort;
import dev.henny.companio.world.Command;
import dev.henny.companio.world.Companion;
import dev.henny.companio.world.CompanionView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CompanioClient {
    private static CompanioClient instance;

    final ConfigStore store;
    final CompanioConfig config;
    final PackRegistry registry = new PackRegistry();
    final PackRepository repository;
    final Senses senses = new Senses();
    final McOutput output;
    final AnimEvents events;
    final OwnCompanion own;
    final RemoteCompanions remote;
    final SyncSender sender = new SyncSender();
    final CompanionTarget target = new CompanionTarget();
    final Bubbles bubbles = new Bubbles();
    final Interactions interactions = new Interactions(this);
    final McSenses mcSenses = new McSenses();
    final Riding riding = new Riding();
    final ClientHooks hooks = new ClientHooks();
    SyncPort port = SyncPort.NONE;
    private McBlockView view;
    private ClientLevel viewLevel;
    private double now;
    private int ticks;
    private List<Pack> lastLoad = List.of();
    private final List<Consumer<List<Pack>>> loadListeners = new ArrayList<>();
    private Runnable screenListener = () -> {};
    private Overlay overlay;

    private CompanioClient(Path gameDir, Supplier<List<Path>> builtinRoots) {
        store = new ConfigStore(gameDir.resolve("config").resolve("companio.json"));
        config = store.load();
        output = new McOutput(() -> config);
        events = new AnimEvents(output, () -> config);
        own = new OwnCompanion(registry, senses, output);
        remote = new RemoteCompanions(registry::matching);
        repository = new PackRepository(registry, gameDir.resolve("companionpacks"), builtinRoots);
        repository.onLoaded(this::loaded);
        repository.reload();
    }

    public static CompanioClient start(Path gameDir, Supplier<List<Path>> builtinRoots) {
        if (instance == null) instance = new CompanioClient(gameDir, builtinRoots);
        return instance;
    }

    public static CompanioClient get() {
        return instance;
    }

    private void loaded(List<Pack> packs) {
        for (Pack p : packs) if (p.builtin() && p.id() != null && config.adoptBuiltin(p.id())) store.markDirty();
        lastLoad = packs;
        for (Consumer<List<Pack>> l : loadListeners) l.accept(packs);
        screenListener.run();
    }

    public void screenListener(Runnable r) {
        screenListener = r == null ? () -> {} : r;
    }

    public void overlay(Overlay o) {
        overlay = o;
    }

    public Overlay overlay() {
        return overlay;
    }

    public void tick(Minecraft mc) {
        repository.tick();
        if (overlay != null) overlay.tick();
        ticks++;
        if (mc.level == null || mc.player == null) {
            leave();
            return;
        }
        now += 0.05;
        if (mc.level != viewLevel) {
            viewLevel = mc.level;
            view = new McBlockView(mc.level);
            remote.clear();
        }
        interactions.ensureListening();
        boolean changed = own.sync(config, mc.player, OwnCompanion::language);
        Companion c = own.get();
        if (changed) sender.reset();
        if (c != null) tickOwn(mc, c);
        else target.remove();
        remote.tick(now, 0.05f);
        if (config.showOthers && config.shown()) for (Puppet p : remote.all()) events.fire(p, true);
        bubbles.update(views(), named(), config.bubbles, (v, p) -> renderPosition(v, p));
        sender.tick(c, registry, mc.level.dimension().identifier().toString(), port, config.share);
        if (ticks % 600 == 0) {
            own.save(config);
            store.save(config);
        } else store.saveIfDirty(config);
    }

    private void tickOwn(Minecraft mc, Companion c) {
        mcSenses.fill(mc.level, mc.player, view, c.body.x, c.body.y, c.body.z, target.hovered(), nearestPuppet(c), senses);
        c.tick(view, now, 0.05f);
        if (c.riding() != null) riding.snap(c, mc.player);
        events.fire(c, true);
        target.update(c, p -> renderPosition(c, p));
        hooks.tick(c);
    }

    private double nearestPuppet(Companion c) {
        double best = Senses.FAR;
        for (Puppet p : remote.all()) best = Math.min(best, c.body.distance(p.body().x, p.body().y, p.body().z));
        return best;
    }

    private Set<CompanionView> named() {
        Set<CompanionView> s = new HashSet<>();
        Companion c = own.get();
        if (c != null && !c.name().isEmpty() && (target.hovered() || c.held())) s.add(c);
        for (Puppet p : remote.all()) if (!p.name().isEmpty()) s.add(p);
        return s;
    }

    private boolean renderPosition(CompanionView v, double[] out) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        CompanionRenderer.position(this, v, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false), out);
        return true;
    }

    public boolean ridingPosition(CompanionView v, float pt, double[] out) {
        Companion c = own.get();
        return v == c && c.riding() != null && riding.position(c, Minecraft.getInstance().player, pt, out);
    }

    private void leave() {
        if (own.get() != null) {
            own.clear(config);
            store.save(config);
        }
        target.remove();
        bubbles.clear();
        remote.clear();
        viewLevel = null;
        view = null;
    }

    public List<CompanionView> views() {
        List<CompanionView> out = new ArrayList<>();
        if (!config.shown()) return out;
        Companion c = own.get();
        if (c != null) out.add(c);
        if (config.showOthers) out.addAll(remote.all());
        return out;
    }

    public Companion own() {
        return own.get();
    }

    public CompanionTarget target() {
        return target;
    }

    public CompanioConfig config() {
        return config;
    }

    public PackRegistry registry() {
        return registry;
    }

    public PackRepository repository() {
        return repository;
    }

    public List<Pack> lastLoad() {
        return lastLoad;
    }

    public void onPacksLoaded(Consumer<List<Pack>> listener) {
        loadListeners.add(listener);
    }

    public void changed() {
        store.markDirty();
    }

    public void setSize(float size) {
        Companion c = own.get();
        config.size = c == null ? size : c.def.size().clamp(size);
        changed();
    }

    public void rename(String name) {
        config.name = dev.henny.companio.text.Sanitize.name(name);
        Companion c = own.get();
        if (c != null) c.rename(config.name);
        changed();
    }

    public void command(Command cmd) {
        config.command = cmd.id();
        Companion c = own.get();
        if (c != null) c.command(cmd);
        changed();
    }

    public void openMenu() {
        hooks.openMenu();
    }

    public ClientHooks hooks() {
        return hooks;
    }

    public void port(SyncPort p) {
        port = p == null ? SyncPort.NONE : p;
        sender.reset();
    }

    public void receive(UUID player, String payload) {
        remote.receive(player, payload, now);
    }

    public void forget(UUID player) {
        remote.remove(player);
    }

    public double now() {
        return now;
    }
}
