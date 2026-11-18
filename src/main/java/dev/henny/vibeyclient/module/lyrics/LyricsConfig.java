package dev.henny.vibeyclient.module.lyrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;

/** Everything the player can change (ported from Vibey Lyrics), saved in config/vibeyclient/lyrics.json. */
public final class LyricsConfig {
    public enum Position { SKY, FRONT, WORLD, HUD }

    /** How the words of a line show up. */
    public enum Reveal { LINE, WORD }

    /**
     * How a line comes in (and, for some, how its words move while sung). FLIGHT and DRIFT place every word on its
     * own in 3D: FLIGHT builds a tilted ribbon that flies past you when the next line starts, DRIFT is the calm one.
     */
    public enum Anim { FLIGHT, DRIFT, SCATTER, POP, FADE, SLIDE, ZOOM, WAVE, BOUNCE, TYPE, GLITCH, RAINBOW, NONE;

        public boolean is3d() {
            return this == FLIGHT || this == DRIFT;
        }
    }

    public enum HudPlace { TOP, CENTER, BOTTOM }

    /**
     * Where sky lyrics sit. SOFT stays put while you look around a little and glides after you when you turn away or
     * look down while walking; FOLLOW is glued (smoothly) to your view; FIXED stays in one compass direction.
     */
    public enum SkyMode { SOFT, FOLLOW, FIXED }

    /** Fonts shipped with the mod (assets/vibeyclient/font/lyrics) plus the two vanilla ones. */
    public static final String[] FONTS = {
            "default", "uniform", "vibeyclient:lyrics/bebas", "vibeyclient:lyrics/bungee", "vibeyclient:lyrics/marker",
            "vibeyclient:lyrics/pacifico", "vibeyclient:lyrics/pressstart", "vibeyclient:lyrics/monoton", "vibeyclient:lyrics/poppins",
            "vibeyclient:lyrics/montserrat", "vibeyclient:lyrics/kanit", "vibeyclient:lyrics/fredoka", "vibeyclient:lyrics/unbounded",
            "vibeyclient:lyrics/caveat", "vibeyclient:lyrics/brush"
    };

    /** Ready-made looks; picking one in the main menu sets font, animation and colors. */
    public enum Preset {
        LAVENDER("vibeyclient:lyrics/poppins", Anim.FLIGHT, 0xFFFFFFFF, 0xFFE4D4FF, 0xFF6D28D9, 0, false, false, true),
        BRUSH("vibeyclient:lyrics/brush", Anim.SCATTER, 0xFFF3EEFF, 0xFFFFFFFF, 0xFFB45CFF, 55, false, false, false, true),
        SCATTER("vibeyclient:lyrics/poppins", Anim.SCATTER, 0xFFFFFFFF, 0xFFD8C8FF, 0xFF7C3AED, 45, false, true, false, false),
        DREAMY("vibeyclient:lyrics/poppins", Anim.DRIFT, 0xFFFFEAF2, 0xFFFFFFFF, 0xFFB0408F, 35, false, false, false),
        FIRE("vibeyclient:lyrics/kanit", Anim.FLIGHT, 0xFFFFF1D6, 0xFFFFB347, 0xFFE0115F, 45, false, false, true),
        ICE("vibeyclient:lyrics/montserrat", Anim.DRIFT, 0xFFF0FBFF, 0xFF9BE7FF, 0xFF1D4ED8, 50, false, false, false),
        NEON("vibeyclient:lyrics/bungee", Anim.POP, 0xFFFFFFFF, 0xFFFF4FD8, 0xFF8A2BE2, 70, false, true, false),
        CLASSIC("default", Anim.POP, 0xFFFFFFFF, 0xFFFFD43B, 0xFF000000, 55, false, true, false),
        SUNSET("vibeyclient:lyrics/pacifico", Anim.ZOOM, 0xFFFFE8D6, 0xFFFF7A3D, 0xFFE0115F, 60, false, false, false),
        OCEAN("vibeyclient:lyrics/bebas", Anim.WAVE, 0xFFE0F7FF, 0xFF3BE8FF, 0xFF1D4ED8, 65, false, false, false),
        COMIC("vibeyclient:lyrics/fredoka", Anim.BOUNCE, 0xFFFFFFFF, 0xFFFFD43B, 0xFF1E3A8A, 0, false, false, true),
        SCRIPT("vibeyclient:lyrics/caveat", Anim.FLIGHT, 0xFFFFF7EC, 0xFFFFC2D9, 0xFF9D174D, 40, false, false, false),
        ROYAL("vibeyclient:lyrics/unbounded", Anim.ZOOM, 0xFFFFF7E0, 0xFFFFC94A, 0xFF6B21A8, 55, false, false, true),
        ARCADE("vibeyclient:lyrics/pressstart", Anim.GLITCH, 0xFFF7FFF0, 0xFF4ADE80, 0xFF0F7A35, 70, false, false, false),
        MARKER("vibeyclient:lyrics/marker", Anim.BOUNCE, 0xFFFFFFFF, 0xFFFFD43B, 0xFF000000, 45, false, false, false),
        RETRO("vibeyclient:lyrics/monoton", Anim.RAINBOW, 0xFFFFFFFF, 0xFFFF4FD8, 0xFF8A2BE2, 55, false, false, false),
        MINIMAL("default", Anim.FADE, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0, true, true, false);

        public final String font;
        public final Anim anim;
        public final int text;
        public final int highlight;
        public final int glow;
        public final int glowStrength;
        public final boolean box;
        public final boolean bold;
        /** 3D letters with depth. */
        public final boolean depth;
        /** Pink and cyan edges (neon look). */
        public final boolean chroma;

        Preset(String font, Anim anim, int text, int highlight, int glow, int glowStrength, boolean box, boolean bold, boolean depth) {
            this(font, anim, text, highlight, glow, glowStrength, box, bold, depth, false);
        }

        Preset(String font, Anim anim, int text, int highlight, int glow, int glowStrength, boolean box, boolean bold, boolean depth,
               boolean chroma) {
            this.chroma = chroma;
            this.depth = depth;
            this.font = font;
            this.anim = anim;
            this.text = text;
            this.highlight = highlight;
            this.glow = glow;
            this.glowStrength = glowStrength;
            this.box = box;
            this.bold = bold;
        }
    }

    // main menu
    public String song = "";
    public Position position = Position.SKY;
    public Preset preset = Preset.LAVENDER;
    public int volume = 80;

    // advanced: look
    public String font = Preset.LAVENDER.font;
    public boolean bold = false;
    /** Thick 3D letters with depth behind them. */
    public boolean depth = Preset.LAVENDER.depth;
    /** Lyric colors follow the sky (custom sky, or day / sunset / night of the normal one). */
    public boolean matchSky = false;
    /** Lyric colors from the album art (wins over the sky when there is art). */
    public boolean matchCover = false;
    /** 10 … 100 percent: how see-through the lyrics are. */
    public int opacity = 100;
    /** Hide the HUD lyrics and the bar while an inventory or menu is open (not this mod's menus and the chat). */
    public boolean hideInMenus = true;
    /** Pink and cyan edges around the words. */
    public boolean chroma = false;
    /** The song time next to the now playing bar. */
    public boolean clock = true;
    /** Little sparkles fly off each word the moment it is sung. */
    public boolean sparkles = true;
    public boolean uppercase = false;
    public int textColor = Preset.LAVENDER.text;
    public int highlightColor = Preset.LAVENDER.highlight;
    public int glowColor = Preset.LAVENDER.glow;
    public int glowStrength = Preset.LAVENDER.glowStrength;
    public boolean box = false;
    /** 50 … 300 percent. */
    public int size = 100;
    /** Extra space between rows, in percent of the line height. */
    public int lineSpacing = 30;
    public Reveal reveal = Reveal.LINE;
    public Anim anim = Preset.LAVENDER.anim;
    public boolean showNext = true;
    /** Small "now playing" bar at the top of the screen with the current line. */
    public boolean pill = true;

    // sky
    /** Custom sky, see {@link dev.henny.vibeyclient.module.lyrics.sky.SkyTheme}; "vanilla" = the normal sky. */
    public String sky = "vanilla";
    /** Hide the vanilla clouds while a custom sky is shown (they would be cut off by it). */
    public boolean hideClouds = true;
    /** Shooting stars in night skies. */
    public boolean shootingStars = true;

    // advanced: placement
    public HudPlace hudPlace = HudPlace.CENTER;
    public SkyMode skyMode = SkyMode.SOFT;
    /** Sky lyrics are drawn over hills, trees and roofs instead of disappearing behind them. */
    public boolean skyOnTop = true;
    public float skyYaw = 0f;
    /** Degrees above the horizon. */
    public int skyHeight = 22;
    public int frontDistance = 4;
    public double anchorX;
    public double anchorY = 80;
    public double anchorZ;
    public float anchorYaw;
    public boolean anchorSet = false;
    /** World anchor: true = always faces you, false = fixed like a sign. */
    public boolean anchorBillboard = true;

    // advanced: sync and audio
    /** Milliseconds added to the song position before picking the line (positive = lyrics earlier). */
    public int offsetMs = 0;
    public boolean muteVanillaMusic = true;
    public boolean onlineLookup = true;
    /** Follow the music that plays on this computer (Spotify, browser, ...) and fetch its lyrics. */
    public boolean pcMode = false;

    // ------------------------------------------------------------------------------------------- persistence

    // Stored under "_data.player" of config/vibeyclient/lyrics.json (the module's settings file); the options the
    // Vibey menu shows are typed settings of LyricsModule and mirrored into these fields.
    private static final Gson GSON = new GsonBuilder().create();
    private static LyricsConfig instance;

    public static LyricsConfig get() {
        if (instance == null) instance = new LyricsConfig();
        return instance;
    }

    public void applyPreset(Preset p) {
        preset = p;
        font = p.font;
        anim = p.anim;
        bold = p.bold;
        textColor = p.text;
        highlightColor = p.highlight;
        glowColor = p.glow;
        glowStrength = p.glowStrength;
        box = p.box;
        depth = p.depth;
        chroma = p.chroma;
    }

    /** Startup (module onLoad, after the settings file was read): stored data, else the standalone mod's config. */
    public static void load(LyricsModule module) {
        com.google.gson.JsonElement stored = module.settings().data().get("player");
        LyricsConfig c = null;
        boolean imported = false;
        if (stored != null && stored.isJsonObject()) {
            try {
                c = GSON.fromJson(stored, LyricsConfig.class);
            } catch (Exception e) {
                LyricsModule.LOG.warn("Bad player data in lyrics.json, using defaults: {}", e.toString());
            }
        } else {
            Path legacy = dev.henny.vibeyclient.Vibey.platform().configDir().resolve("vibey-lyrics.json");
            if (Files.isRegularFile(legacy)) {
                try {
                    // Same shape; only the font ids moved to our namespace.
                    String json = Files.readString(legacy).replace("\"vibeylyrics:", "\"vibeyclient:lyrics/");
                    c = GSON.fromJson(json, LyricsConfig.class);
                    imported = c != null;
                    if (imported) LyricsModule.LOG.info("Imported the settings of Vibey Lyrics from {}", legacy);
                } catch (Exception e) {
                    LyricsModule.LOG.warn("Could not import {}: {}", legacy, e.toString());
                }
            }
        }
        if (c == null) c = new LyricsConfig();
        c.sanitize();
        instance = c;
        if (imported) module.copyFrom(c);
        else module.copyInto(c);
        if (imported || stored == null) c.save();
    }

    /** Pushes the values into the module settings and schedules a write of the settings file. */
    public void save() {
        LyricsModule module = LyricsModule.instance();
        if (module == null) return;
        sanitize();
        module.copyFrom(this);
        synchronized (module.settings()) {
            module.settings().data().add("player", GSON.toJsonTree(this));
        }
        module.settings().markDirty();
    }

    private void sanitize() {
        if (position == null) position = Position.SKY;
        if (preset == null) preset = Preset.LAVENDER;
        if (sky == null) sky = "vanilla";
        if (reveal == null) reveal = Reveal.LINE;
        if (anim == null) anim = Anim.POP;
        if (hudPlace == null) hudPlace = HudPlace.CENTER;
        if (skyMode == null) skyMode = SkyMode.SOFT;
        if (font == null || !java.util.Arrays.asList(FONTS).contains(font)) font = "default";
        if (song == null) song = "";
        size = Math.max(50, Math.min(300, size));
        volume = Math.max(0, Math.min(100, volume));
        glowStrength = Math.max(0, Math.min(100, glowStrength));
        skyHeight = Math.max(0, Math.min(60, skyHeight));
        frontDistance = Math.max(2, Math.min(12, frontDistance));
        lineSpacing = Math.max(0, Math.min(100, lineSpacing));
        opacity = Math.max(10, Math.min(100, opacity));
    }
}
