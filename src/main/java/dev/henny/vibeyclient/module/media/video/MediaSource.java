package dev.henny.vibeyclient.module.media.video;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a screen plays. {@link Kind#YOUTUBE} and {@link Kind#WEB} need the browser (MCEF Modern), {@link Kind#FILE}
 * and {@link Kind#STREAM} are decoded by FFmpeg. Local files are personal: they are never sent to other players.
 */
public record MediaSource(Kind kind, String uri) {
    public enum Kind {
        YOUTUBE, WEB, FILE, STREAM;

        public boolean web() {
            return this == YOUTUBE || this == WEB;
        }
    }

    public static final int MAX_URI = 1024;
    private static final Pattern YT_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Set<String> MEDIA_EXT = Set.of("mp4", "m4v", "webm", "mkv", "mov", "avi", "ts", "flv", "m3u8",
            "mpd", "mp3", "ogg", "oga", "opus", "wav", "flac", "m4a", "aac", "gif");
    private static final Set<String> STREAM_SCHEMES = Set.of("rtmp", "rtmps", "rtsp", "srt", "udp", "rtp");

    /** Parses what the user typed or pasted; null if it is not something we can play. */
    public static MediaSource parse(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1).trim();
        if (s.isEmpty() || s.length() > MAX_URI) return null;
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith("file:")) {
            try {
                return file(Path.of(URI.create(s)));
            } catch (RuntimeException e) {
                return null;
            }
        }
        int colon = s.indexOf("://");
        if (colon > 0) {
            String scheme = lower.substring(0, colon);
            URI u;
            try {
                u = URI.create(s.replace(" ", "%20"));
            } catch (IllegalArgumentException e) {
                return null;
            }
            if (u.getHost() == null) return null;
            if (STREAM_SCHEMES.contains(scheme)) return new MediaSource(Kind.STREAM, u.toString());
            if (!scheme.equals("http") && !scheme.equals("https")) return null;
            String yt = youTubeId(u);
            if (yt != null) {
                long start = youTubeStart(u);
                return new MediaSource(Kind.YOUTUBE, "https://www.youtube.com/watch?v=" + yt + (start > 0 ? "&t=" + start : ""));
            }
            return new MediaSource(isMediaPath(u.getPath()) ? Kind.STREAM : Kind.WEB, u.toString());
        }
        if (lower.startsWith("www.") || lower.startsWith("youtube.com") || lower.startsWith("youtu.be")) return parse("https://" + s);
        try {
            Path p = Path.of(s.startsWith("~") ? System.getProperty("user.home") + s.substring(1) : s);
            if (p.isAbsolute() && Files.isRegularFile(p)) return file(p);
        } catch (InvalidPathException ignored) {
            // not a path
        }
        return null;
    }

    private static MediaSource file(Path p) {
        return new MediaSource(Kind.FILE, p.toAbsolutePath().normalize().toString());
    }

    public static boolean isMediaPath(String path) {
        if (path == null) return false;
        int dot = path.lastIndexOf('.');
        return dot >= 0 && MEDIA_EXT.contains(path.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** The 11 character video id of a YouTube watch/share/shorts/embed/live link, else null. */
    public static String youTubeId(URI u) {
        String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.ROOT);
        String path = u.getPath() == null ? "" : u.getPath();
        String id = null;
        if (host.equals("youtu.be")) {
            id = path.length() > 1 ? path.substring(1) : null;
        } else if (host.equals("youtube.com") || host.endsWith(".youtube.com") || host.equals("youtube-nocookie.com")
                || host.endsWith(".youtube-nocookie.com")) {
            if (path.equals("/watch")) id = query(u, "v");
            else for (String prefix : new String[] {"/shorts/", "/embed/", "/live/", "/v/"}) {
                if (path.startsWith(prefix)) id = path.substring(prefix.length());
            }
        }
        if (id == null) return null;
        int slash = id.indexOf('/');
        if (slash >= 0) id = id.substring(0, slash);
        return YT_ID.matcher(id).matches() ? id : null;
    }

    private static long youTubeStart(URI u) {
        String t = query(u, "t");
        if (t == null) t = query(u, "start");
        if (t == null) return 0;
        Matcher m = Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s?)?").matcher(t);
        if (!m.matches()) return 0;
        long h = m.group(1) == null ? 0 : Long.parseLong(m.group(1));
        long min = m.group(2) == null ? 0 : Long.parseLong(m.group(2));
        long sec = m.group(3) == null ? 0 : Long.parseLong(m.group(3));
        return h * 3600 + min * 60 + sec;
    }

    private static String query(URI u, String key) {
        String q = u.getRawQuery();
        if (q == null) return null;
        for (String part : q.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0 && part.substring(0, eq).equals(key)) return part.substring(eq + 1);
        }
        return null;
    }

    /** Video id (YouTube only). */
    public String youTubeId() {
        return kind == Kind.YOUTUBE ? youTubeId(URI.create(uri)) : null;
    }

    /** Start offset in seconds from a {@code t=} parameter (YouTube only). */
    public long youTubeStartSeconds() {
        return kind == Kind.YOUTUBE ? youTubeStart(URI.create(uri)) : 0;
    }

    /** May go to other players: everything except local files (and only http/https for streams). */
    public boolean shareable() {
        if (kind == Kind.FILE) return false;
        String lower = uri.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    /** Short text for the UI: host for URLs, file name for files. */
    public String label() {
        if (kind == Kind.FILE) {
            Path p = Path.of(uri);
            return p.getFileName() == null ? uri : p.getFileName().toString();
        }
        try {
            URI u = URI.create(uri);
            String host = u.getHost() == null ? uri : u.getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IllegalArgumentException e) {
            return uri;
        }
    }
}
