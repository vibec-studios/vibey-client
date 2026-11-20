package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Ease;
import dev.henny.vibeyclient.module.cinematic.core.Pose;
import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public final class Director {
    public static final double FADE = 0.8, CUT = 0.35;

    private final Map<Mode, Planner> planners = new EnumMap<>(Mode.class);
    private final Planner still = new StillPlanner();
    private final Random rnd;
    private Mode chosen = Mode.MIX;
    private Mode playing = Mode.SELF;
    private double mixEvery = 40;
    private double mixClock;
    private int mixIndex = -1;
    private Shot shot;
    private boolean checked;
    private double t;
    private double end;
    private double outLen = FADE;
    private Pose pose;
    private Pose lastGood;
    private double fade = 1;
    private int frame;
    private int shots;

    public Director(long seed) {
        rnd = new Random(seed);
        planners.put(Mode.BUILDS, new BuildsPlanner());
        planners.put(Mode.PLAYERS, new PlayersPlanner());
        planners.put(Mode.SELF, new SelfPlanner());
        planners.put(Mode.PANORAMA, new PanoramaPlanner());
        planners.put(Mode.WAYPOINTS, new WaypointsPlanner());
        planners.put(Mode.SKY, new SkyPlanner());
        planners.put(Mode.DRONIE, new DroniePlanner());
    }

    public void mode(Mode m) {
        if (m == chosen) return;
        chosen = m;
        if (shot != null) cut();
    }

    public void mixSeconds(double s) {
        mixEvery = Math.max(10, s);
    }

    public Pose update(Scene s, double dt) {
        if (shot == null) next(s);
        t += Math.max(0, dt);
        mixClock += Math.max(0, dt);
        if (chosen == Mode.MIX && mixClock >= mixEvery) cut();
        Pose p = shot.at(Math.min(t, shot.duration()));
        if (!checked || safe(s, p)) lastGood = p;
        else {
            p = lastGood != null ? lastGood : p;
            cut();
        }
        pose = p;
        fade = Math.max(1 - t / FADE, 1 - (end - t) / outLen);
        fade = Ease.clamp01(fade);
        if (t >= end) next(s);
        return pose;
    }

    private boolean safe(Scene s, Pose p) {
        if (!ShotCheck.pose(s, p)) return false;
        return frame++ % 4 != 0 || ShotCheck.visible(s, p.pos());
    }

    public void cut() {
        if (end > t + CUT) {
            end = t + CUT;
            outLen = CUT;
        }
    }

    private void next(Scene s) {
        Mode m = chosen == Mode.MIX ? mixPick(s) : chosen;
        Shot planned = tryPlan(m, s);
        playing = m;
        checked = true;
        if (planned == null && m != Mode.SELF) planned = tryPlan(Mode.SELF, s);
        if (planned == null) {
            planned = still.plan(s, rnd);
            checked = false;
        }
        shot = planned;
        shots++;
        t = 0;
        end = shot.duration();
        outLen = FADE;
        lastGood = null;
    }

    private Shot tryPlan(Mode m, Scene s) {
        Planner p = planners.get(m);
        if (p == null || !p.possible(s)) return null;
        return p.plan(s, rnd);
    }

    private Mode mixPick(Scene s) {
        if (mixIndex >= 0 && mixClock < mixEvery) return Mode.MIXED[mixIndex];
        mixClock = 0;
        for (int i = 1; i <= Mode.MIXED.length; i++) {
            int k = Math.floorMod(mixIndex + i, Mode.MIXED.length);
            Mode m = Mode.MIXED[k];
            if (m == Mode.SKY && !s.sky().night() && rnd.nextInt(3) != 0) continue;
            if (planners.get(m).possible(s)) {
                mixIndex = k;
                return m;
            }
        }
        mixIndex = Math.max(0, mixIndex);
        return Mode.SELF;
    }

    public Pose pose() {
        return pose;
    }

    public double fade() {
        return fade;
    }

    public Mode chosen() {
        return chosen;
    }

    public Mode playing() {
        return playing;
    }

    public String subject() {
        return shot == null ? "" : shot.subject();
    }

    public int shots() {
        return shots;
    }

    public Shot shot() {
        return shot;
    }

    public double shotTime() {
        return t;
    }

    public BuildsPlanner builds() {
        return (BuildsPlanner) planners.get(Mode.BUILDS);
    }
}
