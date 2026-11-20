package dev.henny.companio.world;

import dev.henny.companio.anim.Animator;
import dev.henny.companio.anim.LookControl;
import dev.henny.companio.brain.BehaviorRun;
import dev.henny.companio.brain.Brain;
import dev.henny.companio.brain.Self;
import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.def.LocoState;
import dev.henny.companio.def.LookMode;
import dev.henny.companio.def.MoveMode;
import dev.henny.companio.def.PropDef;
import dev.henny.companio.move.Angles;
import dev.henny.companio.move.BlockView;
import dev.henny.companio.move.Body;
import dev.henny.companio.move.MoveIntent;
import dev.henny.companio.move.Mover;
import dev.henny.companio.move.Physics;
import dev.henny.companio.move.Terrain;
import dev.henny.companio.sense.EventDetector;
import dev.henny.companio.sense.Owner;
import dev.henny.companio.sense.Senses;
import dev.henny.companio.text.Sanitize;
import dev.henny.cosmetico.model.Bundle;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public final class Companion implements CompanionView {
    public final CompanionDef def;
    public final Body body = new Body();
    public final Bubble bubble = new Bubble();
    public final Animator animator;
    public final LookControl look;
    final Random random;
    final Senses senses;
    final Self self = new Self();
    final Mover mover;
    final Brain brain;
    final Map<String, Boolean> props = new HashMap<>();
    private final EventDetector events = new EventDetector();
    private final MoveIntent intent = new MoveIntent();
    Output output = Output.NONE;
    double now, born = Double.NaN;
    float hopRequest;
    private String variant, name;
    private float size;
    private Command command = Command.FOLLOW;
    private boolean held;
    private MoveMode mode = MoveMode.FOLLOW;
    private LocoState loco = LocoState.IDLE;

    public Companion(CompanionDef def, Senses senses, long seed, Supplier<String> language) {
        this.def = def;
        this.senses = senses;
        this.random = new Random(seed);
        this.variant = def.defaultVariant();
        this.name = "";
        this.mover = new Mover(def.movement(), random);
        this.brain = new Brain(def, senses, self, random, () -> new CompanionFx(this), language);
        this.animator = new Animator(def.model(variant));
        this.look = new LookControl(def.look(), random);
        for (PropDef p : def.props().values()) props.put(p.id(), p.shown());
        brain.props(this::propShown);
        size(def.size().def());
    }

    public void spawn(double x, double y, double z, float yaw) {
        body.place(x, y, z);
        body.yaw = body.pyaw = yaw;
        brain.fire("spawn");
    }

    public void tick(BlockView view, double now, float dt) {
        this.now = now;
        if (Double.isNaN(born)) born = now;
        fillSelf();
        events.detect(senses, body.inWater, body.onGround, body.y, brain::fire);
        brain.tick(now, dt);
        BehaviorRun run = brain.current();
        mode = run == null || run.move() == null ? Modes.idle(command) : Modes.resolve(run.move(), command, run.def.obeys());
        body.beginTick();
        if (held) intent.clear();
        else move(view, dt);
        turn(run, dt);
        loco = Locomotion.of(body, mode, held, mover.riding() != null, def.movement());
        boolean still = body.speed() < 0.35 && !held && mover.riding() == null;
        String base = run != null && run.def.baseAnim() != null && still ? run.def.baseAnim() : def.loco().get(loco);
        animator.base(base, Locomotion.speedFactor(body, loco, def.movement()), dt);
        animator.tick(dt);
    }

    private void move(BlockView view, float dt) {
        Terrain t = new Terrain(view, body.height, def.movement().canSwim());
        mover.tick(mode, senses, body, t, intent, now, dt);
        if (hopRequest > 0 && body.onGround) {
            intent.jump = true;
            intent.jumpHeight = hopRequest;
            hopRequest = 0;
        }
        if (mover.riding() == null) Physics.step(body, view, intent, def.movement().step());
        if (mover.teleported()) brain.fire("teleport");
    }

    private void turn(BehaviorRun run, float dt) {
        Owner o = senses.owner;
        boolean moving = body.speed() > 0.3 && mover.riding() == null && !held;
        if (moving) body.yaw = Angles.approach(body.yaw, Angles.yawTo(body.vx, body.vz), def.movement().turnSpeed() * dt);
        else if (mover.riding() != null) body.yaw = o.bodyYaw;
        LookMode lm = run != null && run.look() != null ? run.look() : !moving && o.present && body.distance(o.x, o.y, o.z) < 8 ? LookMode.PLAYER : LookMode.MOVE;
        if (held || mover.riding() != null) lm = lm == LookMode.PLAYER ? LookMode.AROUND : lm;
        float eye = def.box().height() * size * 0.8f;
        float turnBody = look.tick(lm, body.yaw, body.x, body.y + eye, body.z, o.x, o.eyeY, o.z, o.present, now, dt);
        if (!moving && !held && mover.riding() == null && (turnBody != 0 || (lm == LookMode.PLAYER && def.look().bone() == null))) {
            float target = Angles.yawTo(o.x - body.x, o.z - body.z);
            body.yaw = Angles.approach(body.yaw, target, def.movement().turnSpeed() * 0.6f * dt);
        }
    }

    private void fillSelf() {
        Self s = self;
        s.speed = body.speed();
        s.moving = s.speed > 0.35;
        s.onGround = body.onGround;
        s.inWater = body.inWater;
        s.onIce = body.onGround && body.ground == dev.henny.companio.move.Surface.ICE;
        s.onSnow = body.onGround && body.ground == dev.henny.companio.move.Surface.SNOW;
        s.held = held;
        s.riding = mover.riding() != null;
        s.command = command.id();
        s.size = size;
        s.variant = variant;
        s.age = now - born;
        s.x = body.x;
        s.y = body.y;
        s.z = body.z;
        s.sky = senses.sky;
    }

    public boolean propShown(String id) {
        BehaviorRun run = brain.current();
        if (run != null && run.def.props().containsKey(id)) return run.def.props().get(id);
        return props.getOrDefault(id, false);
    }

    void setProp(String id, boolean show) {
        if (props.containsKey(id)) props.put(id, show);
    }

    public boolean[] hiddenBones(boolean[] out) {
        Bundle b = model();
        int n = b == null ? 0 : b.model.bones.length;
        if (out == null || out.length < n) out = new boolean[n];
        java.util.Arrays.fill(out, false);
        for (PropDef p : def.props().values()) if (!propShown(p.id())) for (int i : p.boneIndices()) if (i < n) out[i] = true;
        return out;
    }

    public void fire(String event) {
        brain.fire(event);
    }

    public void command(Command c) {
        command(c, true);
    }

    public void command(Command c, boolean announce) {
        command = c;
        if (c == Command.STAY) mover.home.set(body.x, body.y, body.z);
        else mover.home.clear();
        if (announce) brain.fire("command_" + c.id());
    }

    public void pet() {
        brain.fire("pet");
    }

    public void pickUp() {
        held = true;
        mover.forceDismount(body, new Terrain(EmptyView.INSTANCE, body.height, true), senses.owner);
        brain.fire("pick_up");
    }

    public void holdAt(double x, double y, double z) {
        body.x = x;
        body.y = y;
        body.z = z;
    }

    public void place(double x, double y, double z) {
        held = false;
        body.place(x, y, z);
        if (command == Command.STAY) mover.home.set(x, y, z);
        brain.fire("place");
    }

    public void rename(String newName) {
        name = Sanitize.name(newName);
        brain.fire("rename");
    }

    public boolean trick(String id) {
        var t = def.trick(id);
        boolean ok = t != null && brain.trick(t.behavior());
        if (!ok) bubble.show("?", now, 1.5f);
        return ok;
    }

    public void teleportToOwner(BlockView view) {
        mover.teleport(body, new Terrain(view, body.height, def.movement().canSwim()), senses.owner);
        brain.fire("teleport");
    }

    public void variant(String v) {
        if (def.variants().stream().noneMatch(x -> x.id().equals(v)) && !"default".equals(v)) return;
        variant = v;
        animator.bundle(def.model(v));
    }

    public void size(float s) {
        size = def.size().clamp(s);
        body.width = def.box().width() * size;
        body.height = def.box().height() * size;
    }

    public void output(Output o) {
        output = o == null ? Output.NONE : o;
    }

    public void name(String n) {
        name = Sanitize.name(n);
    }

    public Bundle model() {
        return def.model(variant);
    }

    public CompanionDef def() { return def; }
    public Body body() { return body; }
    public Animator animator() { return animator; }
    public float headYaw(float partial) { return look.yaw(partial); }
    public float headPitch(float partial) { return look.pitch(partial); }
    public String bubbleText() { return bubble.text(now); }

    public String variant() { return variant; }
    public String name() { return name; }
    public float size() { return size; }
    public Command command() { return command; }
    public boolean held() { return held; }
    public MoveMode mode() { return mode; }
    public MoveMode riding() { return mover.riding(); }
    public LocoState loco() { return loco; }
    public Brain brain() { return brain; }
    public double now() { return now; }
    public Mover mover() { return mover; }
}
