package dev.henny.r3.sync;

import com.google.gson.JsonObject;
import dev.henny.r3.fx.Rng;
import dev.henny.r3.persist.PlacementCodec;
import dev.henny.r3.persist.PlacementRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class SharedObjects implements SyncReceiver {
    private final String kind;
    private final SyncChannel channel;
    private final SyncPolicy policy;
    private final SharedObjectsListener listener;
    private final UUID self;
    private final LongSupplier clockMs;
    private final Rng rng;
    private final RateLimiter limiter;
    private final RevisionClock revisions;
    private final Map<String, PlacementRecord> published = new LinkedHashMap<>();
    private final Map<String, RemoteEntry> remote = new LinkedHashMap<>();
    private final Set<UUID> askers = new LinkedHashSet<>();
    private long answerAt;

    public SharedObjects(String kind, SyncChannel channel, SyncPolicy policy, SharedObjectsListener listener,
                         UUID self, LongSupplier clockMs, long seed) {
        this.kind = kind;
        this.channel = channel;
        this.policy = policy;
        this.listener = listener;
        this.self = self;
        this.clockMs = clockMs;
        this.rng = new Rng(seed);
        this.limiter = new RateLimiter(policy.burst(), policy.messagesPerSecond(), clockMs);
        this.revisions = new RevisionClock(clockMs);
        channel.receiver(this);
    }

    public long publish(PlacementRecord record) {
        record.revision = revisions.next(record.revision);
        record.owner = self.toString();
        published.put(record.id, record);
        if (channel.available()) channel.broadcast(SyncMessage.upsert(kind, record));
        return record.revision;
    }

    public void publishState(String id, JsonObject payload) {
        PlacementRecord r = published.get(id);
        if (r == null) return;
        r.revision = revisions.next(r.revision);
        if (channel.available()) channel.broadcast(SyncMessage.state(kind, id, r.revision, payload));
    }

    public void unpublish(String id) {
        PlacementRecord r = published.remove(id);
        if (r != null && channel.available()) channel.broadcast(SyncMessage.remove(kind, id, revisions.next(r.revision)));
    }

    public void requestAll() {
        if (channel.available()) channel.broadcast(SyncMessage.request(kind));
    }

    public void tick() {
        if (answerAt == 0 || clockMs.getAsLong() < answerAt) return;
        answerAt = 0;
        if (channel.available()) {
            for (UUID who : askers) for (PlacementRecord r : published.values()) channel.sendTo(who, SyncMessage.upsert(kind, r));
        }
        askers.clear();
    }

    @Override
    public void receive(UUID sender, SyncMessage m) {
        if (sender == null || sender.equals(self) || m == null || !kind.equals(m.kind())) return;
        if (!limiter.allow(sender)) return;
        switch (m.op()) {
            case UPSERT -> upsert(sender, m);
            case STATE -> state(sender, m);
            case REMOVE -> remove(sender, m);
            case REQUEST -> request(sender);
        }
    }

    private void upsert(UUID sender, SyncMessage m) {
        PlacementRecord r = m.placement();
        if (!PlacementCodec.valid(r) || !r.id.equals(m.objectId()) || !kind.equals(r.kind) || !policy.accept().test(r)) return;
        if (published.containsKey(r.id)) return;
        RemoteEntry entry = remote.get(r.id);
        if (entry != null && (!entry.owner().equals(sender) || m.revision() <= entry.revision())) return;
        if (entry == null && countOwnedBy(sender) >= policy.maxObjectsPerOwner()) return;
        r.owner = sender.toString();
        r.revision = m.revision();
        if (entry == null) remote.put(r.id, new RemoteEntry(sender, r, m.revision()));
        else entry.update(r, m.revision());
        listener.upserted(sender, r, entry == null);
    }

    private void state(UUID sender, SyncMessage m) {
        RemoteEntry entry = remote.get(m.objectId());
        if (entry == null || !entry.owner().equals(sender) || m.revision() <= entry.revision()) return;
        entry.update(null, m.revision());
        listener.state(sender, m.objectId(), m.payload());
    }

    private void remove(UUID sender, SyncMessage m) {
        RemoteEntry entry = remote.get(m.objectId());
        if (entry == null || !entry.owner().equals(sender) || m.revision() < entry.revision()) return;
        remote.remove(m.objectId());
        listener.removed(sender, m.objectId());
    }

    private void request(UUID sender) {
        askers.add(sender);
        if (answerAt == 0) {
            long span = Math.max(0, policy.answerDelayMaxMs() - policy.answerDelayMinMs());
            answerAt = clockMs.getAsLong() + policy.answerDelayMinMs() + (long) (rng.nextFloat() * span);
        }
    }

    public void forgetOwner(UUID owner) {
        List<String> gone = new ArrayList<>();
        remote.forEach((id, e) -> {
            if (e.owner().equals(owner)) gone.add(id);
        });
        for (String id : gone) {
            remote.remove(id);
            listener.removed(owner, id);
        }
        limiter.forget(owner);
    }

    public void clear() {
        remote.clear();
        published.clear();
        askers.clear();
        answerAt = 0;
        limiter.clear();
    }

    public int countOwnedBy(UUID owner) {
        int n = 0;
        for (RemoteEntry e : remote.values()) if (e.owner().equals(owner)) n++;
        return n;
    }

    public Map<String, RemoteEntry> remote() {
        return Collections.unmodifiableMap(remote);
    }

    public Map<String, PlacementRecord> published() {
        return Collections.unmodifiableMap(published);
    }

    public long pendingAnswerAt() {
        return answerAt;
    }
}
