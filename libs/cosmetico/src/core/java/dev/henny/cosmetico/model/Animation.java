package dev.henny.cosmetico.model;

public final class Animation {
    public final String name;
    public final float length;
    public final boolean loop, hold;
    public final Track[] tracks;
    public final Event[] events;
    public final boolean hasVisibility;

    public Animation(String name, float length, boolean loop, boolean hold, Track[] tracks, Event[] events) {
        this.name = name;
        this.length = length;
        this.loop = loop;
        this.hold = hold;
        this.tracks = tracks;
        this.events = events;
        boolean vis = false;
        for (Event e : events) if (e.type == EventType.VISIBILITY) vis = true;
        this.hasVisibility = vis;
    }

    public Track track(PlayerBone b) {
        for (Track t : tracks) if (t.playerBone() == b) return t;
        return null;
    }
}
