package dev.henny.cosmetico.model;

public final class Event {
    public final float t;
    public final EventType type;
    public final String id;
    public final int sprite, sound;
    public final Locator locator;
    public final int count;
    public final float[] spread, velocity;
    public final float lifetime, size, gravity;
    public final boolean emissive;
    public final float volume, pitch;
    public final int bone;
    public final boolean visible;

    public Event(float t, EventType type, String id, int sprite, int sound, Locator locator, int count, float[] spread, float[] velocity,
                 float lifetime, float size, float gravity, boolean emissive, float volume, float pitch, int bone, boolean visible) {
        this.t = t;
        this.type = type;
        this.id = id;
        this.sprite = sprite;
        this.sound = sound;
        this.locator = locator;
        this.count = count;
        this.spread = spread;
        this.velocity = velocity;
        this.lifetime = lifetime;
        this.size = size;
        this.gravity = gravity;
        this.emissive = emissive;
        this.volume = volume;
        this.pitch = pitch;
        this.bone = bone;
        this.visible = visible;
    }
}
