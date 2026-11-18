package dev.henny.vibeyclient.avatar.bundle;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.avatar.Rarity;
import dev.henny.vibeyclient.avatar.Slot;

import java.util.List;
import java.util.Map;

/**
 * A parsed, validated {@code .vibey} bundle (SPEC §5). Immutable and thread safe; built by {@link BundleParser} on a
 * worker thread. All coordinates are kept in Blockbench space exactly as written; the renderer converts them to
 * Minecraft model space ({@code x' = -x, y' = 24 - y, z' = z}, rotations {@code (-x, -y, z)}).
 */
public final class Bundle {
    public enum Kind { COSMETIC, EMOTE }

    /** Where a root bone is attached. */
    public enum Attach {
        HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG, ROOT, WORLD;

        public static Attach of(String s) {
            if (s == null) return null;
            for (Attach a : values()) if (a.name().equalsIgnoreCase(s)) return a;
            return null;
        }

        /** The player part, or null for ROOT/WORLD. */
        public PlayerBone part() {
            return switch (this) {
                case HEAD -> PlayerBone.HEAD;
                case BODY -> PlayerBone.BODY;
                case RIGHT_ARM -> PlayerBone.RIGHT_ARM;
                case LEFT_ARM -> PlayerBone.LEFT_ARM;
                case RIGHT_LEG -> PlayerBone.RIGHT_LEG;
                case LEFT_LEG -> PlayerBone.LEFT_LEG;
                default -> null;
            };
        }
    }

    /** The bones of the vanilla player an emote can animate, with their rest pivots in Blockbench space. */
    public enum PlayerBone {
        HEAD("head", 0, 24, 0), BODY("body", 0, 24, 0), RIGHT_ARM("right_arm", 5, 22, 0), LEFT_ARM("left_arm", -5, 22, 0),
        RIGHT_LEG("right_leg", 1.9f, 12, 0), LEFT_LEG("left_leg", -1.9f, 12, 0), ROOT("root", 0, 0, 0);

        public static final PlayerBone[] VALUES = values();
        public final String id;
        public final float px, py, pz;

        PlayerBone(String id, float px, float py, float pz) {
            this.id = id;
            this.px = px;
            this.py = py;
            this.pz = pz;
        }

        public static PlayerBone of(String s) {
            for (PlayerBone b : VALUES) if (b.id.equals(s)) return b;
            return null;
        }
    }

    public enum Armor { HEAD, CHEST, LEGS, FEET }

    public enum PetFollow {
        GROUND, SHOULDER_RIGHT, SHOULDER_LEFT, FLOAT;

        public static PetFollow of(String s) {
            if (s != null) for (PetFollow f : values()) if (f.name().equalsIgnoreCase(s)) return f;
            return GROUND;
        }
    }

    public enum Interp { LINEAR, CATMULLROM, STEP, BEZIER }

    /** {@code {"en": "...", "de": "..."}}. */
    public record Text(String en, String de) {
        public static final Text EMPTY = new Text("", null);

        public String get() {
            if (Lang.german() && de != null && !de.isEmpty()) return de;
            return en == null ? "" : en;
        }
    }

    public static final class Texture {
        public final String id;
        /** UV size of one frame. */
        public final int width, height;
        public final byte[] png;
        public final int pngWidth, pngHeight;
        /** Frames in the vertical strip (1 = not animated). */
        public final int frameCount;
        /** Ticks per frame. */
        public final int frameTime;
        /** Frame order (indices into the strip). */
        public final int[] frames;
        public final boolean interpolate;

        Texture(String id, int width, int height, byte[] png, int pngWidth, int pngHeight, int frameCount, int frameTime, int[] frames, boolean interpolate) {
            this.id = id;
            this.width = width;
            this.height = height;
            this.png = png;
            this.pngWidth = pngWidth;
            this.pngHeight = pngHeight;
            this.frameCount = frameCount;
            this.frameTime = frameTime;
            this.frames = frames;
            this.interpolate = interpolate;
        }

        public boolean animated() {
            return frameCount > 1 && frames.length > 1;
        }

        /** Total animation length in ticks. */
        public int cycleTicks() {
            return Math.max(1, frames.length * frameTime);
        }
    }

    /** One face of a cube: UV rectangle in the texture's UV space and the texture index. */
    public record Face(float u1, float v1, float u2, float v2, int texture) {}

    public static final class Cube {
        /** Blockbench space, absolute. */
        public final float[] from, to;
        public final float inflate;
        public final boolean mirror;
        /** Per-cube rotation (Blockbench degrees) about {@link #pivot}; null = none. */
        public final float[] pivot, rotation;
        /** Box UV origin, or null for per-face UV. */
        public final float[] box;
        /** Texture of the box UV cube (or default for faces). */
        public final int texture;
        /** north, east, south, west, up, down; null entries are not drawn (per-face UV only). */
        public final Face[] faces;

        Cube(float[] from, float[] to, float inflate, boolean mirror, float[] pivot, float[] rotation, float[] box, int texture, Face[] faces) {
            this.from = from;
            this.to = to;
            this.inflate = inflate;
            this.mirror = mirror;
            this.pivot = pivot;
            this.rotation = rotation;
            this.box = box;
            this.texture = texture;
            this.faces = faces;
        }
    }

    public static final class Bone {
        public final String name;
        public final int index;
        /** Index of the parent bone (parents always come first), -1 for root bones. */
        public final int parent;
        /** Root bones only (null on children). */
        public final Attach attach;
        public final float[] pivot, rotation;
        public final Cube[] cubes;
        public final boolean emissive, translucent;
        /** Emissive/translucent inherited from the parents. */
        public final boolean effectiveEmissive, effectiveTranslucent;

        Bone(String name, int index, int parent, Attach attach, float[] pivot, float[] rotation, Cube[] cubes,
             boolean emissive, boolean translucent, boolean effectiveEmissive, boolean effectiveTranslucent) {
            this.name = name;
            this.index = index;
            this.parent = parent;
            this.attach = attach;
            this.pivot = pivot;
            this.rotation = rotation;
            this.cubes = cubes;
            this.emissive = emissive;
            this.translucent = translucent;
            this.effectiveEmissive = effectiveEmissive;
            this.effectiveTranslucent = effectiveTranslucent;
        }
    }

    /** A point for particles/sounds. Exactly one of {@code bone} (model bone index) / {@code playerBone} is set. */
    public record Locator(String name, int bone, PlayerBone playerBone, float[] position) {}

    public static final class Model {
        public final int texture;
        public final Bone[] bones;
        public final Map<String, Locator> locators;
        public final int cubeCount;

        Model(int texture, Bone[] bones, Map<String, Locator> locators, int cubeCount) {
            this.texture = texture;
            this.bones = bones;
            this.locators = locators;
            this.cubeCount = cubeCount;
        }

        public Bone bone(String name) {
            for (Bone b : bones) if (b.name.equals(name)) return b;
            return null;
        }
    }

    /** Keyframes of one channel: {@code t[i]}, value {@code v[3i..3i+2]}, optional {@code pre} (NaN = none). */
    public static final class Channel {
        public final float[] t;
        public final float[] v;
        public final float[] pre;
        public final Interp[] interp;

        Channel(float[] t, float[] v, float[] pre, Interp[] interp) {
            this.t = t;
            this.v = v;
            this.pre = pre;
            this.interp = interp;
        }

        public int size() {
            return t.length;
        }
    }

    /** Animation of one bone: model bone ({@code bone >= 0}) or player bone. Missing channels are null. */
    public record Track(int bone, PlayerBone playerBone, Channel rotation, Channel position, Channel scale) {}

    public enum EventType { PARTICLE, SOUND, VISIBILITY }

    public static final class Event {
        public final float t;
        public final EventType type;
        /** particle: vanilla id or null; sound: id (vanilla or embedded). */
        public final String id;
        /** Sprite index for sprite particles, -1 otherwise; embedded sound index or -1. */
        public final int sprite, sound;
        public final Locator locator;
        public final int count;
        public final float[] spread, velocity;
        public final float lifetime, size, gravity;
        public final boolean emissive;
        public final float volume, pitch;
        /** visibility: model bone index and value. */
        public final int bone;
        public final boolean visible;

        Event(float t, EventType type, String id, int sprite, int sound, Locator locator, int count, float[] spread, float[] velocity,
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

    public static final class Animation {
        public final String name;
        public final float length;
        public final boolean loop, hold;
        public final Track[] tracks;
        /** Sorted by time. */
        public final Event[] events;
        /** Per model bone: visibility at t = 0 (after events at t = 0), used as the start state. */
        public final boolean hasVisibility;

        Animation(String name, float length, boolean loop, boolean hold, Track[] tracks, Event[] events) {
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
            for (Track t : tracks) if (t.playerBone == b) return t;
            return null;
        }
    }

    /** Sprite for sprite particles: UV rect in the texture's UV space. */
    public record Sprite(String id, int texture, float u1, float v1, float u2, float v2) {}

    public record Sound(String id, byte[] ogg) {}

    public record EmoteOptions(String animation, boolean loop, boolean thirdPerson, boolean stopOnMove, boolean hideHeldItems) {}

    public record PetOptions(PetFollow follow, float scale) {}

    /**
     * {@code cosmetic.offsetWhen} rule: while an item in {@code slot} is worn, the root bones of this item move by
     * {@code offset} (Blockbench px) and turn by {@code rotation} (degrees, Blockbench convention).
     */
    public record OffsetRule(Slot slot, float[] offset, float[] rotation) {}

    // ============================================================================================ fields

    public final String id;
    public final Kind kind;
    public final Text name, description;
    public final String author;
    public final List<String> tags;
    public final Rarity rarity;
    /** Cosmetics only (null for emotes). */
    public final Slot slot;
    public final java.util.Set<Armor> hideWithArmor;
    public final PetOptions pet;
    /** Texture index of the cape / shield texture, -1 if none. */
    public final int capeTexture, shieldTexture;
    /** Emotes only. */
    public final EmoteOptions emote;
    public final Texture[] textures;
    public final Model model;
    public final Map<String, Animation> animations;
    public final Sprite[] sprites;
    public final Sound[] sounds;
    /** SHA-256 (hex) of the file bytes. */
    public final String sha256;
    public final int byteSize;
    public final List<String> warnings;
    /** {@code cosmetic.offsetWhen} rules (empty if none). */
    public final List<OffsetRule> offsetWhen;

    Bundle(String id, Kind kind, Text name, Text description, String author, List<String> tags, Rarity rarity, Slot slot,
           java.util.Set<Armor> hideWithArmor, PetOptions pet, int capeTexture, int shieldTexture, EmoteOptions emote,
           Texture[] textures, Model model, Map<String, Animation> animations, Sprite[] sprites, Sound[] sounds,
           String sha256, int byteSize, List<String> warnings, List<OffsetRule> offsetWhen) {
        this.id = id;
        this.kind = kind;
        this.name = name;
        this.description = description;
        this.author = author;
        this.tags = tags;
        this.rarity = rarity;
        this.slot = slot;
        this.hideWithArmor = hideWithArmor;
        this.pet = pet;
        this.capeTexture = capeTexture;
        this.shieldTexture = shieldTexture;
        this.emote = emote;
        this.textures = textures;
        this.model = model;
        this.animations = animations;
        this.sprites = sprites;
        this.sounds = sounds;
        this.sha256 = sha256;
        this.byteSize = byteSize;
        this.warnings = warnings;
        this.offsetWhen = offsetWhen == null ? List.of() : offsetWhen;
    }

    public Animation animation(String name) {
        return animations.get(name);
    }

    /** The emote's main animation (emotes only). */
    public Animation emoteAnimation() {
        return emote == null ? null : animations.get(emote.animation());
    }

    public boolean isEmote() {
        return kind == Kind.EMOTE;
    }
}
