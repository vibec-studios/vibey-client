package dev.henny.vibeyclient.module.clips;

import dev.henny.vibeyclient.Vibey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Automatic clips from what the vanilla client receives anyway (docs/CLIPS.md §5): a player we damaged in the last
 * 8 s dies → kill; ≥ 2 kills within 10 s → multi-kill; a kill at ≤ 3 hearts or the kill of the last of ≥ 2 players
 * who hit us in the last 15 s → clutch; optionally our own death. The clip is saved after the post-roll; events in the
 * post-roll extend it into one clip. Client thread only. Nothing is sent, nothing is shown but the "Clip saved" toast.
 */
public final class AutoClips {
    static final long HIT_WINDOW_MS = 8_000, MULTI_WINDOW_MS = 10_000, ATTACKER_WINDOW_MS = 15_000;
    static final long DEDUPE_MS = 2_000, COOLDOWN_MS = 10_000;
    static final float CLUTCH_HP = 6f;

    private final ClipsModule m;
    /** entity id → when we last damaged that player, and their name. */
    private final Map<Integer, Hit> ourHits = new HashMap<>();
    /** entity id → when that player last damaged us. */
    private final Map<Integer, Hit> attackers = new HashMap<>();
    private final Map<String, Long> recentKills = new HashMap<>();
    private Pending pending;
    private long lastSaveAt;
    private int savedThisSession;
    private boolean wasDead;
    /** For tests and the stats overlay. */
    public int kills, triggers;

    private record Hit(long at, String name) {}

    private static final class Pending {
        final List<Long> times = new ArrayList<>();
        final List<String> types = new ArrayList<>();
        final List<String> victims = new ArrayList<>();
        int kills;
        boolean clutch;
        boolean death;
        long saveAt;
        long highlightAt;
    }

    AutoClips(ClipsModule m) {
        this.m = m;
    }

    void reset() {
        ourHits.clear();
        attackers.clear();
        recentKills.clear();
        pending = null;
        savedThisSession = 0;
        wasDead = false;
    }

    // ============================================================================================ signals (mixins)

    /** {@code ClientboundDamageEventPacket}: {@code victim} was damaged, caused by {@code cause} (directly {@code direct}). */
    public void onDamage(Entity victim, int causeId, int directId) {
        Minecraft mc = Minecraft.getInstance();
        if (!m.autoActive() || mc.player == null || victim == null) return;
        int me = mc.player.getId();
        long now = System.currentTimeMillis();
        if ((causeId == me || directId == me) && victim != mc.player && victim instanceof Player p) {
            ourHits.put(p.getId(), new Hit(now, p.getName().getString()));
        } else if (victim == mc.player && causeId >= 0 && causeId != me) {
            Entity src = mc.level == null ? null : mc.level.getEntity(causeId);
            if (src instanceof Player p) attackers.put(p.getId(), new Hit(now, p.getName().getString()));
        }
    }

    /** Our own melee hit (backup for servers that don't broadcast damage events to the attacker). */
    public void onAttack(Entity target) {
        if (!m.autoActive() || !(target instanceof Player p) || p == Minecraft.getInstance().player) return;
        ourHits.put(p.getId(), new Hit(System.currentTimeMillis(), p.getName().getString()));
    }

    /** Entity event 3 (death animation) for a living entity. */
    public void onDeathEvent(Entity e) {
        Minecraft mc = Minecraft.getInstance();
        if (!m.autoActive() || mc.player == null || !(e instanceof Player p) || p == mc.player) return;
        Hit h = ourHits.remove(p.getId());
        long now = System.currentTimeMillis();
        if (h != null && now - h.at <= HIT_WINDOW_MS) kill(h.name, now);
        else attackerDied(p.getId());
    }

    /**
     * A system chat line; translated death messages ({@code death.attack.*} with us as the killer argument) are the
     * second kill signal.
     */
    public void onGameMessage(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (!m.autoActive() || mc.player == null || message == null) return;
        if (!(message.getContents() instanceof TranslatableContents t) || !t.getKey().startsWith("death.")) return;
        Object[] args = t.getArgs();
        if (args.length < 2) return;
        String victim = text(args[0]);
        String killer = text(args[1]);
        String me = mc.player.getName().getString();
        if (victim == null || killer == null || !killer.equals(me) || victim.equals(me)) return;
        kill(victim, System.currentTimeMillis());
    }

    private static String text(Object o) {
        if (o instanceof Component c) return c.getString();
        return o == null ? null : o.toString();
    }

    // ============================================================================================ logic

    private void attackerDied(int id) {
        Hit a = attackers.get(id);
        if (a != null) attackers.put(id, new Hit(-a.at, a.name)); // negative = dead, keeps the time
    }

    void kill(String victim, long now) {
        Long seen = recentKills.get(victim);
        if (seen != null && now - seen < DEDUPE_MS) return; // entity event + death message for the same kill
        recentKills.put(victim, now);
        kills++;
        Minecraft mc = Minecraft.getInstance();
        boolean lowHp = mc.player != null && mc.player.getHealth() <= CLUTCH_HP;
        // the last of >= 2 recent attackers?
        int recent = 0, alive = 0;
        for (Iterator<Map.Entry<Integer, Hit>> it = attackers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Hit> en = it.next();
            long at = Math.abs(en.getValue().at);
            if (now - at > ATTACKER_WINDOW_MS) {
                it.remove();
                continue;
            }
            recent++;
            boolean dead = en.getValue().at < 0 || en.getValue().name.equals(victim);
            if (!dead) alive++;
        }
        boolean lastOfMany = recent >= 2 && alive == 0;
        for (Map.Entry<Integer, Hit> en : attackers.entrySet()) {
            if (en.getValue().name.equals(victim) && en.getValue().at > 0) en.setValue(new Hit(-en.getValue().at, victim));
        }
        event("kill", victim, now, lowHp || lastOfMany);
    }

    private void event(String type, String victim, long now, boolean clutch) {
        if (pending == null) {
            if (now - lastSaveAt < COOLDOWN_MS) return;
            if (savedThisSession >= m.maxPerSession.get()) return;
            pending = new Pending();
        }
        Pending p = pending;
        if (p.times.size() < 16) {
            p.times.add(now);
            p.types.add(type);
            p.victims.add(victim);
        }
        if (type.equals("kill")) p.kills++;
        if (type.equals("death")) p.death = true;
        p.clutch |= clutch;
        p.highlightAt = now;
        p.saveAt = now + m.postRoll.get() * 1000L;
        triggers++;
    }

    void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean dead = mc.screen instanceof DeathScreen || (mc.player != null && mc.player.isDeadOrDying());
        if (dead && !wasDead && m.autoActive() && m.autoDeath.on()) event("death", null, System.currentTimeMillis(), false);
        wasDead = dead;
        Pending p = pending;
        if (p == null || System.currentTimeMillis() < p.saveAt) return;
        pending = null;
        if (!m.autoActive()) return;
        String reason = reason(p);
        if (reason == null) return;
        savedThisSession++;
        lastSaveAt = System.currentTimeMillis();
        Vibey.LOG.info("Clips: automatic clip ({}, {} kills)", reason, p.kills);
        m.recorder.saveAuto(new ClipSaver.AutoInfo(reason, p.kills, List.copyOf(p.times), List.copyOf(p.types),
                new ArrayList<>(p.victims), p.highlightAt));
    }

    /** Which kind the pending events make, given the switches (null = none of them wanted). */
    String reason(Pending p) {
        if (p.kills > 0) {
            if (p.clutch && m.autoClutch.on()) return "clutch";
            if (p.kills >= 2 && withinMulti(p) && m.autoMultiKill.on()) return "multikill";
            if (m.autoKill.on()) return "kill";
            if (m.autoMultiKill.on() && p.kills >= 2) return "multikill";
        }
        if (p.death && m.autoDeath.on()) return "death";
        return null;
    }

    private static boolean withinMulti(Pending p) {
        long first = -1, last = -1;
        for (int i = 0; i < p.times.size(); i++) {
            if (!p.types.get(i).equals("kill")) continue;
            if (first < 0) first = p.times.get(i);
            last = p.times.get(i);
        }
        return last - first <= MULTI_WINDOW_MS;
    }

    /** Game test: a kill as if the signals had arrived (no second player in a test world). */
    public void testKill(String victim) {
        if (m.autoActive()) kill(victim, System.currentTimeMillis());
    }

    /** A pending clip is waiting for its post-roll (tests). */
    public boolean pending() {
        return pending != null;
    }
}
