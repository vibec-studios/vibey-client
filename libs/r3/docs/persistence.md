# Saving and sharing placements

## Saving

`PlacementCodec.encode(object, data)` turns a `WorldObject` into a Gson record (`PlacementRecord`: id, kind, owner,
dimension, position, quaternion, size, scale, locked, fullBright, revision, anchor, and your feature's `data`
JsonObject). `PlacementStore` keeps records per **world key**:

```java
String key = WorldKeys.server(serverAddress);        // "mp:play.example.org"  (WorldKeys.singleplayer(level) = "sp:<level>")
PlacementStore store = PlacementStore.fromJson(file);  // broken JSON or entries are skipped, never thrown
store.replaceAll(key, "mymod:board", records);
Files.writeString(path, store.toJson());
WorldObject o = PlacementCodec.decode(record, rules.limits());
```

Loading validates: finite coordinates (|x|, |z| ≤ 3.2·10⁷, |y| ≤ 4096), a non-zero quaternion (normalised), a UUID
owner, sizes clamped to the `SizeLimits`, unknown or broken anchors fall back to free. Format version: `"format": 1`.

## Sharing

`SharedObjects` shares one feature kind over a `SyncChannel` the backend provides (e.g. Vibey's server channel).

```java
SharedObjects shared = new SharedObjects("vibey:video", channel,
        SyncPolicy.defaults().accepting(r -> UrlPolicy.acceptRemote(r.data)),
        listener, myUuid, System::currentTimeMillis, seed);
shared.requestAll();                 // on join
shared.publish(record);              // new or changed own object (UPSERT)
shared.publishState(id, payload);    // small state change, e.g. {"playing":true,"pos":1234} (STATE)
shared.unpublish(id);                // REMOVE
shared.tick();                       // answers REQUESTs after a random 0.3–1 s delay
shared.forgetOwner(uuid);            // a player left
```

Messages are JSON (`SyncCodec`, ≤ 8192 chars): `op`, `kind`, `id`, `rev`, `placement`, `payload`.

Receiving rules (`SyncPolicy`): at most 8 objects per owner, token bucket burst 6 / 3 per second per sender, the
feature's `accept` filter, own messages and other kinds ignored, invalid placements dropped.

### Conflict rule

1. **The owner is the authority.** The first valid UPSERT for an id fixes its owner; UPSERT, STATE and REMOVE for that
   id from anyone else are dropped. An id that is one of our own published objects is never overwritten.
2. **Revisions only go up.** Every change carries a revision; a message with a revision ≤ the one we have is stale
   and dropped (reordered or duplicated delivery). Revisions come from `RevisionClock`: `max(previous + 1,
   now_ms × 1024)`, so an owner who restarted the game still wins over its old state.
3. **Whole-state wins, no merging.** An UPSERT replaces the record; a STATE replaces the feature payload. Other
   players never edit a remote object: it arrives with `remote(true)`, so it is locked for grab/resize; local-only
   settings (volume, "load this one") stay in the feature.
4. **Leaving removes.** `forgetOwner` drops a player's objects; REMOVE needs a revision ≥ the known one.

Periodic resends (e.g. STATE every 10 s while a video plays) are the feature's choice: call `publishState`.
