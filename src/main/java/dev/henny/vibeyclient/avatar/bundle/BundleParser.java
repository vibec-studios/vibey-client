package dev.henny.vibeyclient.avatar.bundle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.avatar.Rarity;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.bundle.Bundle.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parses and validates {@code .vibey} v1 (SPEC §5). Thread safe (no state), meant for worker threads. Errors throw
 * {@link BundleException} with a readable message; warnings end up in {@link Bundle#warnings}. Unknown fields are
 * ignored (forward compatible).
 */
public final class BundleParser {
    private BundleParser() {}

    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final int MAX_TEXTURE = 512, MAX_STRIP = 4096, MAX_BONES = 256, MAX_CUBES = 2048, MAX_SOUND = 512 * 1024;
    public static final Pattern ID = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");
    private static final String[] FACE_NAMES = {"north", "east", "south", "west", "up", "down"};

    public static Bundle parse(byte[] bytes) {
        if (bytes.length > MAX_BYTES) throw new BundleException("file is " + bytes.length + " bytes (max " + MAX_BYTES + ")");
        JsonElement root;
        try {
            root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            throw new BundleException("invalid JSON: " + e.getMessage());
        }
        if (!root.isJsonObject()) throw new BundleException("root must be an object");
        return parse(root.getAsJsonObject(), sha256(bytes), bytes.length);
    }

    /** For bundles that arrive already parsed (devlink). The hash is computed over the compact JSON. */
    public static Bundle parse(JsonObject o) {
        byte[] bytes = o.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new BundleException("bundle is " + bytes.length + " bytes (max " + MAX_BYTES + ")");
        return parse(o, sha256(bytes), bytes.length);
    }

    public static String sha256(byte[] bytes) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : d) sb.append(Character.forDigit((b >> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ============================================================================================ main

    private static Bundle parse(JsonObject o, String sha, int size) {
        List<String> warnings = new ArrayList<>();
        if (!"vibey".equals(str(o, "format"))) throw new BundleException("format must be \"vibey\"");
        double fv = num(o, "formatVersion", -1);
        String minClient = str(o, "minClientVersion");
        if (minClient != null && !dev.henny.vibeyclient.avatar.ClientVersion.satisfies(minClient))
            throw new NeedsUpdateException(minClient, "needs Vibey Client " + minClient + " or newer");
        if (fv > 1) throw new NeedsUpdateException(null, "formatVersion " + (long) fv + " needs a newer Vibey Client");
        if (fv != 1) throw new BundleException("formatVersion must be 1");
        String kindS = str(o, "kind");
        Kind kind = "cosmetic".equals(kindS) ? Kind.COSMETIC : "emote".equals(kindS) ? Kind.EMOTE : null;
        if (kind == null) throw new BundleException("kind must be cosmetic or emote");
        String id = str(o, "id");
        if (id == null || id.length() > 128 || !ID.matcher(id).matches()) throw new BundleException("id \"" + id + "\" is not a valid namespace:path id");
        Text name = text(o.get("name"));
        if (name == null || name.en() == null || name.en().isEmpty()) throw new BundleException("name.en is required");
        Text description = text(o.get("description"));
        String author = str(o, "author");
        List<String> tags = new ArrayList<>();
        if (o.get("tags") instanceof JsonArray a) for (JsonElement e : a) if (e.isJsonPrimitive()) tags.add(e.getAsString());
        Rarity rarity = Rarity.of(str(o, "rarity"));

        // textures
        List<Texture> textures = new ArrayList<>();
        Map<String, Integer> texIndex = new HashMap<>();
        if (o.get("textures") instanceof JsonArray ta) {
            for (JsonElement e : ta) {
                if (!(e instanceof JsonObject t)) throw new BundleException("textures entries must be objects");
                Texture tex = texture(t, warnings);
                if (texIndex.put(tex.id, textures.size()) != null) throw new BundleException("duplicate texture id " + tex.id);
                textures.add(tex);
            }
        }

        // sounds / sprites
        List<Sound> sounds = new ArrayList<>();
        Map<String, Integer> soundIndex = new HashMap<>();
        if (o.get("sounds") instanceof JsonArray sa) {
            for (JsonElement e : sa) {
                if (!(e instanceof JsonObject s)) continue;
                String sid = str(s, "id");
                if (sid == null) throw new BundleException("sounds[].id is required");
                byte[] ogg = base64(str(s, "ogg"), "sound " + sid);
                if (ogg.length > MAX_SOUND) throw new BundleException("sound " + sid + " is " + ogg.length + " bytes (max " + MAX_SOUND + ")");
                if (ogg.length < 4 || ogg[0] != 'O' || ogg[1] != 'g' || ogg[2] != 'g' || ogg[3] != 'S') throw new BundleException("sound " + sid + " is not OGG");
                if (soundIndex.put(sid, sounds.size()) != null) throw new BundleException("duplicate sound id " + sid);
                sounds.add(new Sound(sid, ogg));
            }
        }
        List<Sprite> sprites = new ArrayList<>();
        Map<String, Integer> spriteIndex = new HashMap<>();
        if (o.get("sprites") instanceof JsonArray sa) {
            for (JsonElement e : sa) {
                if (!(e instanceof JsonObject s)) continue;
                String sid = str(s, "id");
                Integer ti = texIndex.get(str(s, "texture"));
                if (sid == null) throw new BundleException("sprites[].id is required");
                if (ti == null) throw new BundleException("sprite " + sid + ": texture \"" + str(s, "texture") + "\" does not exist");
                float[] uv = floats(s.get("uv"), 4, "sprite " + sid + " uv");
                if (spriteIndex.put(sid, sprites.size()) != null) throw new BundleException("duplicate sprite id " + sid);
                sprites.add(new Sprite(sid, ti, uv[0], uv[1], uv[2], uv[3]));
            }
        }

        // cosmetic / emote sections
        Slot slot = null;
        Set<Armor> hide = EnumSet.noneOf(Armor.class);
        PetOptions pet = null;
        int cape = -1, shield = -1;
        EmoteOptions emote = null;
        List<OffsetRule> offsetWhen = List.of();
        if (kind == Kind.COSMETIC) {
            if (!(o.get("cosmetic") instanceof JsonObject c)) throw new BundleException("cosmetic section is required");
            slot = Slot.of(str(c, "slot"));
            if (slot == null) throw new BundleException("cosmetic.slot \"" + str(c, "slot") + "\" is unknown");
            if (c.get("hideWithArmor") instanceof JsonArray ha) {
                for (JsonElement e : ha) {
                    try {
                        hide.add(Armor.valueOf(e.getAsString().toUpperCase(Locale.ROOT)));
                    } catch (RuntimeException ex) {
                        warnings.add("hideWithArmor: unknown armor slot " + e);
                    }
                }
            }
            JsonObject p = c.get("pet") instanceof JsonObject po ? po : new JsonObject();
            float scale = (float) num(p, "scale", 1);
            if (!(scale > 0) || scale > 8) throw new BundleException("cosmetic.pet.scale must be a positive number");
            pet = new PetOptions(PetFollow.of(str(p, "follow")), scale);
            offsetWhen = offsetWhen(c.get("offsetWhen"), slot, warnings);
            if (slot == Slot.CAPE) {
                String tid = c.get("cape") instanceof JsonObject co ? str(co, "texture") : null;
                Integer ti = tid == null ? null : texIndex.get(tid);
                if (ti == null) {
                    if (o.get("model") == null) throw new BundleException("cosmetic.cape.texture \"" + tid + "\" does not exist");
                } else cape = ti;
            }
            if (slot == Slot.SHIELD) {
                String tid = c.get("shield") instanceof JsonObject so ? str(so, "texture") : null;
                Integer ti = tid == null ? null : texIndex.get(tid);
                if (ti == null) {
                    if (o.get("model") == null) throw new BundleException("cosmetic.shield.texture \"" + tid + "\" does not exist");
                } else shield = ti;
            }
            if (slot != Slot.CAPE && slot != Slot.SHIELD && !(o.get("model") instanceof JsonObject))
                throw new BundleException("model is required for slot " + slot.id());
        } else {
            if (!(o.get("emote") instanceof JsonObject e)) throw new BundleException("emote section is required");
            String anim = str(e, "animation");
            if (anim == null) anim = "main";
            emote = new EmoteOptions(anim, bool(e, "loop", false), bool(e, "thirdPerson", true), bool(e, "stopOnMove", true), bool(e, "hideHeldItems", true));
        }

        // model
        Model model = null;
        if (o.get("model") instanceof JsonObject m) model = model(m, kind, texIndex, textures, warnings);
        else if (o.has("model") && !o.get("model").isJsonNull()) throw new BundleException("model must be an object");

        // animations
        Map<String, Animation> animations = new LinkedHashMap<>();
        if (o.get("animations") instanceof JsonObject as) {
            for (Map.Entry<String, JsonElement> e : as.entrySet()) {
                if (!(e.getValue() instanceof JsonObject a)) throw new BundleException("animation " + e.getKey() + " must be an object");
                animations.put(e.getKey(), animation(e.getKey(), a, kind, model, soundIndex, spriteIndex, warnings));
            }
        }
        if (emote != null) {
            Animation main = animations.get(emote.animation());
            if (main == null) throw new BundleException("emote.animation \"" + emote.animation() + "\" does not exist");
            // emote.loop equals the animation's loop (SPEC); the animation wins if they disagree.
            if (main.loop != emote.loop()) emote = new EmoteOptions(emote.animation(), main.loop, emote.thirdPerson(), emote.stopOnMove(), emote.hideHeldItems());
        }
        for (String k : new String[]{"price", "cost", "prices"}) if (o.has(k)) throw new BundleException("\"" + k + "\" is not allowed");

        return new Bundle(id, kind, name, description == null ? Text.EMPTY : description, author == null ? "" : author,
                List.copyOf(tags), rarity, slot, Collections.unmodifiableSet(hide), pet, cape, shield, emote,
                textures.toArray(new Texture[0]), model, Collections.unmodifiableMap(animations),
                sprites.toArray(new Sprite[0]), sounds.toArray(new Sound[0]), sha, size, List.copyOf(warnings), offsetWhen);
    }

    /** Max {@code offsetWhen} rules, max |offset| (px) and |rotation| (degrees) per axis. */
    public static final int MAX_OFFSET_RULES = 8;
    public static final float MAX_OFFSET = 64f, MAX_OFFSET_ROTATION = 360f;

    /**
     * {@code cosmetic.offsetWhen: [{ "slot", "offset": [x,y,z], "rotation"?: [x,y,z] }]} (since 0.3.0). Optional and
     * lenient: a malformed rule (or one for a slot this client does not know) is skipped with a warning, the item
     * still loads.
     */
    private static List<OffsetRule> offsetWhen(JsonElement e, Slot own, List<String> warnings) {
        if (e == null || e.isJsonNull()) return List.of();
        if (!(e instanceof JsonArray a)) {
            warnings.add("cosmetic.offsetWhen must be an array (ignored)");
            return List.of();
        }
        List<OffsetRule> out = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            String where = "cosmetic.offsetWhen[" + i + "]";
            if (out.size() >= MAX_OFFSET_RULES) {
                warnings.add("cosmetic.offsetWhen: more than " + MAX_OFFSET_RULES + " rules, the rest is ignored");
                break;
            }
            if (!(a.get(i) instanceof JsonObject r)) {
                warnings.add(where + " must be an object (ignored)");
                continue;
            }
            Slot slot = Slot.of(str(r, "slot"));
            if (slot == null) {
                warnings.add(where + ": unknown slot \"" + str(r, "slot") + "\" (ignored)");
                continue;
            }
            if (slot == own) {
                warnings.add(where + ": refers to the item's own slot (ignored)");
                continue;
            }
            try {
                float[] off = r.has("offset") ? floats(r.get("offset"), 3, where + ".offset") : new float[3];
                float[] rot = r.has("rotation") ? floats(r.get("rotation"), 3, where + ".rotation") : new float[3];
                for (int k = 0; k < 3; k++) {
                    if (Math.abs(off[k]) > MAX_OFFSET) throw new BundleException(where + ".offset must be within ±" + (int) MAX_OFFSET + " px");
                    if (Math.abs(rot[k]) > MAX_OFFSET_ROTATION) throw new BundleException(where + ".rotation must be within ±" + (int) MAX_OFFSET_ROTATION + "°");
                }
                out.add(new OffsetRule(slot, off, rot));
            } catch (BundleException ex) {
                warnings.add(ex.getMessage() + " (ignored)");
            }
        }
        return List.copyOf(out);
    }

    // ============================================================================================ textures

    private static Texture texture(JsonObject t, List<String> warnings) {
        String id = str(t, "id");
        if (id == null || id.isEmpty()) throw new BundleException("textures[].id is required");
        int w = (int) num(t, "width", 0), h = (int) num(t, "height", 0);
        if (w < 1 || h < 1) throw new BundleException("texture " + id + ": width/height must be positive");
        byte[] png = base64(str(t, "png"), "texture " + id);
        if (png.length < 24 || (png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G')
            throw new BundleException("texture " + id + " is not a PNG");
        int pw = readInt(png, 16), ph = readInt(png, 20);
        if (pw < 1 || ph < 1) throw new BundleException("texture " + id + ": bad PNG header");
        if (pw > MAX_TEXTURE) throw new BundleException("texture " + id + " is " + pw + " px wide (max " + MAX_TEXTURE + ")");
        // frame count from the aspect ratio (HD textures keep the aspect of one frame)
        int frames = Math.max(1, Math.round(ph * (float) w / ((float) pw * h)));
        if (frames == 1 && ph > MAX_TEXTURE) throw new BundleException("texture " + id + " is " + ph + " px high (max " + MAX_TEXTURE + ")");
        if (ph > MAX_STRIP) throw new BundleException("texture " + id + " strip is " + ph + " px high (max " + MAX_STRIP + ")");
        int frameTime = 1;
        int[] order = null;
        boolean interpolate = false;
        if (t.get("animation") instanceof JsonObject a) {
            frameTime = Math.max(1, (int) num(a, "frameTime", 1));
            interpolate = bool(a, "interpolate", false);
            if (a.get("frames") instanceof JsonArray fa && !fa.isEmpty()) {
                order = new int[fa.size()];
                for (int i = 0; i < order.length; i++) {
                    int f = fa.get(i).getAsInt();
                    if (f < 0 || f >= frames) throw new BundleException("texture " + id + ": frame " + f + " out of range 0.." + (frames - 1));
                    order[i] = f;
                }
            }
        } else if (frames > 1) {
            warnings.add("texture " + id + " looks like a strip of " + frames + " frames but has no animation");
        }
        if (order == null) {
            order = new int[frames];
            for (int i = 0; i < frames; i++) order[i] = i;
        }
        return new Texture(id, w, h, png, pw, ph, frames, frameTime, order, interpolate);
    }

    private static int readInt(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16) | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }

    // ============================================================================================ model

    private static Model model(JsonObject m, Kind kind, Map<String, Integer> texIndex, List<Texture> textures, List<String> warnings) {
        int defTex = 0;
        String mt = str(m, "texture");
        if (mt != null) {
            Integer i = texIndex.get(mt);
            if (i == null) throw new BundleException("model.texture \"" + mt + "\" does not exist");
            defTex = i;
        }
        JsonArray list = m.get("bones") instanceof JsonArray a ? a : new JsonArray();
        if (list.size() > MAX_BONES) throw new BundleException(list.size() + " bones (max " + MAX_BONES + ")");
        // name -> json, keep file order
        Map<String, JsonObject> byName = new LinkedHashMap<>();
        for (JsonElement e : list) {
            if (!(e instanceof JsonObject b)) throw new BundleException("bones must be objects");
            String n = str(b, "name");
            if (n == null || n.isEmpty()) throw new BundleException("bone name is required");
            if (byName.put(n, b) != null) throw new BundleException("duplicate bone \"" + n + "\"");
            if (kind == Kind.EMOTE && PlayerBone.of(n) != null) throw new BundleException("bone \"" + n + "\" clashes with a player bone name");
        }
        // topological order: parents first
        List<String> order = new ArrayList<>();
        Set<String> done = new HashSet<>();
        for (String n : byName.keySet()) visit(n, byName, done, new HashSet<>(), order);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < order.size(); i++) index.put(order.get(i), i);
        Bone[] bones = new Bone[order.size()];
        int cubes = 0;
        for (int i = 0; i < bones.length; i++) {
            String n = order.get(i);
            JsonObject b = byName.get(n);
            String parentName = str(b, "parent");
            int parent = parentName == null ? -1 : index.get(parentName);
            Attach attach = null;
            if (parent < 0) {
                String at = str(b, "attach");
                attach = Attach.of(at);
                if (attach == null) {
                    // unknown (future) attach points degrade to the default instead of rejecting the whole item
                    attach = kind == Kind.EMOTE ? Attach.ROOT : Attach.BODY;
                    warnings.add("bone \"" + n + "\" " + (at == null ? "has no attach" : "has unknown attach \"" + at + "\"")
                            + ", using " + attach.name().toLowerCase(Locale.ROOT));
                }
            }
            float[] pivot = vec(b.get("pivot"), "bone " + n + " pivot", 0);
            float[] rot = vec(b.get("rotation"), "bone " + n + " rotation", 0);
            boolean emissive = bool(b, "emissive", false), translucent = bool(b, "translucent", false);
            boolean effE = emissive || (parent >= 0 && bones[parent].effectiveEmissive);
            boolean effT = translucent || (parent >= 0 && bones[parent].effectiveTranslucent);
            List<Cube> cl = new ArrayList<>();
            if (b.get("cubes") instanceof JsonArray ca) {
                for (JsonElement ce : ca) {
                    if (!(ce instanceof JsonObject c)) throw new BundleException("bone " + n + ": cubes must be objects");
                    Cube cube = cube(n, c, defTex, texIndex, textures, warnings);
                    if (cube != null) cl.add(cube);
                    cubes++;
                }
            }
            bones[i] = new Bone(n, i, parent, attach, pivot, rot, cl.toArray(new Cube[0]), emissive, translucent, effE, effT);
        }
        if (cubes > MAX_CUBES) throw new BundleException(cubes + " cubes (max " + MAX_CUBES + ")");
        Map<String, Locator> locators = new LinkedHashMap<>();
        if (m.get("locators") instanceof JsonObject lo) {
            for (Map.Entry<String, JsonElement> e : lo.entrySet()) {
                if (!(e.getValue() instanceof JsonObject l)) throw new BundleException("locator " + e.getKey() + " must be an object");
                String bone = str(l, "bone");
                Integer bi = bone == null ? null : index.get(bone);
                PlayerBone pb = bi == null ? PlayerBone.of(bone) : null;
                if (bi == null && pb == null) throw new BundleException("locator \"" + e.getKey() + "\": bone \"" + bone + "\" does not exist");
                locators.put(e.getKey(), new Locator(e.getKey(), bi == null ? -1 : bi, pb, vec(l.get("position"), "locator " + e.getKey(), 0)));
            }
        }
        return new Model(defTex, bones, Collections.unmodifiableMap(locators), cubes);
    }

    private static void visit(String n, Map<String, JsonObject> byName, Set<String> done, Set<String> stack, List<String> out) {
        if (done.contains(n)) return;
        if (!stack.add(n)) throw new BundleException("bone \"" + n + "\": parent cycle");
        String p = str(byName.get(n), "parent");
        if (p != null) {
            if (!byName.containsKey(p)) throw new BundleException("bone \"" + n + "\": parent \"" + p + "\" does not exist");
            visit(p, byName, done, stack, out);
        }
        stack.remove(n);
        done.add(n);
        out.add(n);
    }

    private static Cube cube(String bone, JsonObject c, int defTex, Map<String, Integer> texIndex, List<Texture> textures, List<String> warnings) {
        String where = "bone " + bone + " cube";
        float[] from = vec(c.get("from"), where + " from", Float.NaN);
        float[] to = vec(c.get("to"), where + " to", Float.NaN);
        if (Float.isNaN(from[0]) || Float.isNaN(to[0])) throw new BundleException(where + ": from/to are required");
        // normalise so from <= to
        for (int k = 0; k < 3; k++) {
            if (from[k] > to[k]) {
                float t = from[k];
                from[k] = to[k];
                to[k] = t;
            }
        }
        float inflate = (float) num(c, "inflate", 0);
        boolean mirror = bool(c, "mirror", false);
        float[] rot = c.has("rotation") ? vec(c.get("rotation"), where + " rotation", 0) : null;
        if (rot != null && rot[0] == 0 && rot[1] == 0 && rot[2] == 0) rot = null;
        float[] pivot = rot == null ? null : vec(c.get("pivot"), where + " pivot", 0);
        int tex = defTex;
        String ct = str(c, "texture");
        if (ct != null) {
            Integer i = texIndex.get(ct);
            if (i == null) throw new BundleException(where + ": texture \"" + ct + "\" does not exist");
            tex = i;
        }
        if (textures.isEmpty()) throw new BundleException(where + ": bundle has no textures");
        if (!(c.get("uv") instanceof JsonObject uv)) throw new BundleException(where + ": uv is required");
        if (uv.has("box")) {
            float[] box = floats(uv.get("box"), 2, where + " uv.box");
            return new Cube(from, to, inflate, mirror, pivot, rot, box, tex, null);
        }
        Face[] faces = new Face[6];
        boolean any = false;
        for (int f = 0; f < 6; f++) {
            JsonElement fe = uv.get(FACE_NAMES[f]);
            if (fe == null || fe.isJsonNull()) continue;
            float[] r;
            int ft = tex;
            if (fe instanceof JsonObject fo) {
                r = floats(fo.get("uv"), 4, where + " uv." + FACE_NAMES[f]);
                String t = str(fo, "texture");
                if (t != null) {
                    Integer i = texIndex.get(t);
                    if (i == null) throw new BundleException(where + " uv." + FACE_NAMES[f] + ": texture \"" + t + "\" does not exist");
                    ft = i;
                }
            } else {
                r = floats(fe, 4, where + " uv." + FACE_NAMES[f]);
            }
            faces[f] = new Face(r[0], r[1], r[2], r[3], ft);
            any = true;
        }
        if (!any) {
            warnings.add(where + " has no faces");
            return null;
        }
        return new Cube(from, to, inflate, mirror, pivot, rot, null, tex, faces);
    }

    // ============================================================================================ animations

    private static Animation animation(String name, JsonObject a, Kind kind, Model model, Map<String, Integer> sounds,
                                       Map<String, Integer> sprites, List<String> warnings) {
        float length = (float) num(a, "length", 0);
        if (!(length > 0)) throw new BundleException("animation " + name + ": length must be > 0");
        boolean loop = bool(a, "loop", false);
        boolean hold = bool(a, "hold", false) && !loop;
        List<Track> tracks = new ArrayList<>();
        if (a.get("bones") instanceof JsonObject bo) {
            for (Map.Entry<String, JsonElement> e : bo.entrySet()) {
                String bn = e.getKey();
                Bone mb = model == null ? null : model.bone(bn);
                PlayerBone pb = mb == null && kind == Kind.EMOTE ? PlayerBone.of(bn) : null;
                if (mb == null && pb == null) {
                    warnings.add("animation " + name + ": bone \"" + bn + "\" does not exist");
                    continue;
                }
                if (!(e.getValue() instanceof JsonObject ch)) continue;
                String where = "animation " + name + " bone " + bn;
                Channel r = channel(ch.get("rotation"), length, where + " rotation", warnings);
                Channel p = channel(ch.get("position"), length, where + " position", warnings);
                Channel s = channel(ch.get("scale"), length, where + " scale", warnings);
                if (r == null && p == null && s == null) continue;
                tracks.add(new Track(mb == null ? -1 : mb.index, pb, r, p, s));
            }
        }
        List<Event> events = new ArrayList<>();
        if (a.get("events") instanceof JsonArray ea) {
            for (JsonElement ee : ea) {
                if (!(ee instanceof JsonObject e)) continue;
                Event ev = event(name, e, length, model, sounds, sprites, warnings);
                if (ev != null) events.add(ev);
            }
        }
        events.sort(Comparator.comparingDouble(e -> e.t));
        return new Animation(name, length, loop, hold, tracks.toArray(new Track[0]), events.toArray(new Event[0]));
    }

    private static Channel channel(JsonElement el, float length, String where, List<String> warnings) {
        if (!(el instanceof JsonArray arr) || arr.isEmpty()) return null;
        int n = arr.size();
        float[] t = new float[n], v = new float[n * 3], pre = new float[n * 3];
        Interp[] in = new Interp[n];
        float last = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (!(arr.get(i) instanceof JsonObject k)) throw new BundleException(where + ": keyframes must be objects");
            float kt = (float) num(k, "t", Double.NaN);
            if (Float.isNaN(kt)) throw new BundleException(where + ": keyframe without t");
            if (kt < last) throw new BundleException(where + ": keyframes must be sorted by t");
            last = kt;
            t[i] = kt;
            float[] val = vec(k.get("v"), where + " v", Float.NaN);
            if (Float.isNaN(val[0])) throw new BundleException(where + ": keyframe without v");
            System.arraycopy(val, 0, v, i * 3, 3);
            if (k.has("pre")) System.arraycopy(vec(k.get("pre"), where + " pre", 0), 0, pre, i * 3, 3);
            else Arrays.fill(pre, i * 3, i * 3 + 3, Float.NaN);
            String is = str(k, "i");
            in[i] = is == null ? Interp.LINEAR : switch (is) {
                case "linear" -> Interp.LINEAR;
                case "catmullrom" -> Interp.CATMULLROM;
                case "step" -> Interp.STEP;
                case "bezier" -> Interp.BEZIER;
                default -> {
                    String w = where + ": unknown interpolation \"" + is + "\", using linear";
                    if (!warnings.contains(w)) warnings.add(w);
                    yield Interp.LINEAR;
                }
            };
        }
        return new Channel(t, v, pre, in);
    }

    private static Event event(String anim, JsonObject e, float length, Model model, Map<String, Integer> sounds,
                               Map<String, Integer> sprites, List<String> warnings) {
        String where = "animation " + anim + " event";
        float t = (float) num(e, "t", 0);
        String type = str(e, "type");
        Locator loc = null;
        String ln = str(e, "locator");
        if (ln != null) {
            loc = model == null ? null : model.locators.get(ln);
            if (loc == null) throw new BundleException(where + ": locator \"" + ln + "\" does not exist");
        }
        if ("particle".equals(type)) {
            String sprite = str(e, "sprite");
            String particle = str(e, "particle");
            int si = -1;
            if (sprite != null) {
                Integer i = sprites.get(sprite);
                if (i == null) throw new BundleException(where + ": sprite \"" + sprite + "\" does not exist");
                si = i;
                particle = null;
            } else if (particle == null) {
                throw new BundleException(where + ": particle or sprite is required");
            } else if (!particle.contains(":")) {
                particle = "minecraft:" + particle;
            }
            int count = Math.max(1, Math.min(64, (int) num(e, "count", 1)));
            return new Event(t, EventType.PARTICLE, particle, si, -1, loc, count,
                    vec(e.get("spread"), where + " spread", 0), vec(e.get("velocity"), where + " velocity", 0),
                    (float) num(e, "lifetime", 1), (float) num(e, "size", 0.15), (float) num(e, "gravity", 0),
                    bool(e, "emissive", false), 1, 1, -1, true);
        }
        if ("sound".equals(type)) {
            String sound = str(e, "sound");
            if (sound == null) throw new BundleException(where + ": sound is required");
            Integer si = sounds.get(sound);
            if (si == null && !sound.contains(":")) throw new BundleException(where + ": embedded sound \"" + sound + "\" does not exist");
            return new Event(t, EventType.SOUND, sound, -1, si == null ? -1 : si, loc, 1, null, null, 0, 0, 0, false,
                    (float) num(e, "volume", 1), (float) num(e, "pitch", 1), -1, true);
        }
        if ("visibility".equals(type)) {
            String bone = str(e, "bone");
            Bone b = model == null || bone == null ? null : model.bone(bone);
            if (b == null) throw new BundleException(where + ": bone \"" + bone + "\" does not exist");
            return new Event(t, EventType.VISIBILITY, null, -1, -1, null, 0, null, null, 0, 0, 0, false, 0, 0, b.index, bool(e, "visible", true));
        }
        warnings.add(where + ": unknown type " + type);
        return null;
    }

    // ============================================================================================ json helpers

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static double num(JsonObject o, String k, double def) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return def;
        double d = e.getAsDouble();
        return Double.isFinite(d) ? d : def;
    }

    private static boolean bool(JsonObject o, String k, boolean def) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : def;
    }

    private static Text text(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive()) return new Text(e.getAsString(), null);
        if (!(e instanceof JsonObject o)) return null;
        return new Text(str(o, "en"), str(o, "de"));
    }

    private static float[] vec(JsonElement e, String where, float def) {
        if (e == null || e.isJsonNull()) return new float[]{def, def, def};
        return floats(e, 3, where);
    }

    private static float[] floats(JsonElement e, int n, String where) {
        if (!(e instanceof JsonArray a) || a.size() != n) throw new BundleException(where + " must be an array of " + n + " numbers");
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            JsonElement x = a.get(i);
            if (!x.isJsonPrimitive() || !x.getAsJsonPrimitive().isNumber()) throw new BundleException(where + " must contain numbers");
            out[i] = x.getAsFloat();
            if (!Float.isFinite(out[i])) throw new BundleException(where + " must contain finite numbers");
        }
        return out;
    }

    private static byte[] base64(String s, String where) {
        if (s == null) throw new BundleException(where + ": data is missing");
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException e) {
            throw new BundleException(where + ": invalid base64");
        }
    }
}
