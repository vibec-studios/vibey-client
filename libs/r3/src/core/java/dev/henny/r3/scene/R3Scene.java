package dev.henny.r3.scene;

import dev.henny.r3.anchor.BlockHit;
import dev.henny.r3.anchor.WorldQuery;
import dev.henny.r3.draw.HandleDrawer;
import dev.henny.r3.draw.ObjectDrawer;
import dev.henny.r3.fx.Effect;
import dev.henny.r3.fx.EffectBudget;
import dev.henny.r3.fx.EffectContext;
import dev.henny.r3.hint.HintStore;
import dev.henny.r3.input.GestureBindings;
import dev.henny.r3.input.HeldKeys;
import dev.henny.r3.input.InputCode;
import dev.henny.r3.interact.Interactable;
import dev.henny.r3.interact.Interaction;
import dev.henny.r3.interact.InteractionConfig;
import dev.henny.r3.interact.Pointer;
import dev.henny.r3.math.Ray;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.render.WorldCanvas;
import dev.henny.r3.view.CameraState;
import dev.henny.r3.view.ViewFrustum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class R3Scene {
    public static final double BLOCK_HIT_TOLERANCE = 0.05;

    private final WorldQuery query;
    private final Interaction interaction;
    private final EffectBudget budget;
    private final Map<String, WorldObject> objects = new LinkedHashMap<>();
    private final List<WorldObject> ordered = new ArrayList<>();
    private final List<WorldObject> interactive = new ArrayList<>();
    private final List<WorldObject> drawList = new ArrayList<>();
    private final List<WorldObject> finished = new ArrayList<>();
    private final ObjectUpdater updater = new ObjectUpdater();
    private final ObjectDrawer drawer = new ObjectDrawer();
    private final HandleDrawer handles = new HandleDrawer();
    private final EffectContext effects = new EffectContext();
    private final CameraState camera = new CameraState();
    private final Pointer pointer = new Pointer();
    private final BlockHit blockHit = new BlockHit();
    private double reach = Pointer.DEFAULT_REACH;

    public R3Scene(WorldQuery query, Interaction interaction, EffectBudget budget) {
        this.query = query;
        this.interaction = interaction.query(query);
        this.budget = budget;
    }

    public static R3Scene create(WorldQuery query, HeldKeys held, HintStore hints) {
        Interaction interaction = new Interaction(InteractionConfig.defaults(), GestureBindings.defaults(), held, hints);
        return new R3Scene(query, interaction, EffectBudget.standard());
    }

    public WorldObject add(WorldObject object) {
        WorldObject old = objects.put(object.id(), object);
        if (old != null) {
            ordered.remove(old);
            interaction.forget(old);
        }
        ordered.add(object);
        return object;
    }

    public WorldObject remove(String id) {
        WorldObject o = objects.remove(id);
        if (o != null) {
            ordered.remove(o);
            interactive.remove(o);
            drawList.remove(o);
            interaction.forget(o);
        }
        return o;
    }

    public WorldObject get(String id) {
        return objects.get(id);
    }

    public List<WorldObject> objects() {
        return Collections.unmodifiableList(ordered);
    }

    public void clear() {
        for (WorldObject o : new ArrayList<>(ordered)) remove(o.id());
    }

    public R3Scene reach(double blocks) {
        reach = blocks;
        return this;
    }

    public void update(CameraState cam, ViewFrustum frustum, float dt) {
        double max = reach;
        if (query != null && query.raycastBlocks(cam.position(), cam.look(), reach, blockHit)) {
            max = blockHit.distance() + BLOCK_HIT_TOLERANCE;
        }
        update(cam, frustum, pointer.fromCrosshair(cam, max), dt);
    }

    public void update(CameraState cam, ViewFrustum frustum, Ray ray, double maxDistance, float dt) {
        update(cam, frustum, pointer.fromRay(ray, cam, maxDistance), dt);
    }

    private void update(CameraState cam, ViewFrustum frustum, Pointer current, float dt) {
        camera.set(cam);
        budget.newFrame();
        effects.camera(camera);
        updater.begin(query, camera, frustum, budget, dt);
        interactive.clear();
        drawList.clear();
        finished.clear();
        Interactable held = interaction.held();
        for (int i = 0; i < ordered.size(); i++) {
            WorldObject o = ordered.get(i);
            boolean visible = updater.update(o, o == held, effects);
            if (visible) drawList.add(o);
            if (visible && o.hoverable()) interactive.add(o);
            if (o.content() instanceof Effect e && e.finished()) finished.add(o);
        }
        for (int i = 0; i < finished.size(); i++) remove(finished.get(i).id());
        interaction.update(current, interactive, dt);
    }

    public void tick() {
        for (int i = 0; i < ordered.size(); i++) {
            WorldObject o = ordered.get(i);
            if (o.content() != null) o.content().tick();
        }
    }

    public void render(WorldCanvas canvas) {
        drawList.sort(DrawOrder.INSTANCE);
        for (int i = 0; i < drawList.size(); i++) {
            WorldObject o = drawList.get(i);
            if (o.content() instanceof Effect) {
                effects.origin(o.transform().position(), o.transform().orientation())
                        .detail(budget.lodAt(o.state().distance()), o.state().alpha(), o.state().light(), budget);
            }
            drawer.draw(canvas, o, effects);
        }
        Interactable held = interaction.held();
        if (held != null) {
            handles.drawOutline(canvas, held, camera.distanceTo(held.transform().position()));
        } else if (interaction.hover().target() != null) {
            handles.drawHover(canvas, interaction.hover(), interaction.hover().hit().distance());
        }
    }

    public boolean press(InputCode code) {
        return interaction.press(code);
    }

    public boolean release(InputCode code) {
        return interaction.release(code);
    }

    public boolean wheel(double notches) {
        return interaction.wheel(notches);
    }

    public boolean blocksVanillaMouse() {
        return interaction.capturing() || interaction.hover().target() != null;
    }

    public boolean blocksHotbarScroll() {
        Interactable t = interaction.hover().target();
        return interaction.capturing() || t != null && !t.locked() && t.rules().resizable();
    }

    public Interaction interaction() {
        return interaction;
    }

    public EffectBudget budget() {
        return budget;
    }

    public List<WorldObject> visibleObjects() {
        return Collections.unmodifiableList(drawList);
    }

    public WorldQuery query() {
        return query;
    }
}
