package dev.henny.vibeyclient.module.media.video.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Finds the FFmpeg program (our own download first, then the PATH) and, on Windows and only after the user agreed,
 * downloads the LGPL "shared" build of github.com/BtbN/FFmpeg-Builds, checked against the release's checksums.
 * FFmpeg always runs as a separate process; nothing is linked into the game.
 */
public final class Ffmpeg {
    public static final String SOURCE = "github.com/BtbN/FFmpeg-Builds";
    public static final String LICENSE = "LGPL-2.1-or-later";
    private static final String RELEASE_API = "https://api.github.com/repos/BtbN/FFmpeg-Builds/releases/latest";
    private static final Pattern ASSET = Pattern.compile("ffmpeg-n(\\d+)\\.(\\d+)-latest-%s-lgpl-shared-\\d+\\.\\d+\\.zip");

    private static volatile String found;
    private static volatile boolean searched;
    /** Dev test of the setup prompt: ignore an FFmpeg on the PATH (only our own download counts). */
    public static volatile boolean hiddenForTest;

    private Ffmpeg() {}

    public static Path ownDir() {
        return FabricLoader.getInstance().getGameDir().resolve("vibeyclient").resolve("ffmpeg");
    }

    private static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String exe(String name) {
        return windows() ? name + ".exe" : name;
    }

    /** Path of the ffmpeg executable, or null. */
    public static String find() {
        if (searched) return found;
        String f = null;
        Path own = ownDir().resolve("bin").resolve(exe("ffmpeg"));
        if (Files.isRegularFile(own)) f = own.toString();
        if (f == null && !hiddenForTest) {
            String path = System.getenv("PATH");
            if (path != null) {
                for (String dir : path.split(java.io.File.pathSeparator)) {
                    if (dir.isBlank()) continue;
                    try {
                        Path p = Path.of(dir.trim()).resolve(exe("ffmpeg"));
                        if (Files.isRegularFile(p) && Files.isExecutable(p)) {
                            f = p.toString();
                            break;
                        }
                    } catch (RuntimeException ignored) {
                        // odd PATH entry
                    }
                }
            }
        }
        if (f == null && !windows() && !hiddenForTest) {
            for (String p : new String[] {"/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg", "/opt/homebrew/bin/ffmpeg"}) {
                if (Files.isExecutable(Path.of(p))) {
                    f = p;
                    break;
                }
            }
        }
        found = f;
        searched = true;
        return f;
    }

    public static boolean available() {
        return find() != null;
    }

    public static void refresh() {
        searched = false;
    }

    /** We can offer a download here (Windows x64/arm64); elsewhere the prompt explains the system package. */
    public static boolean downloadable() {
        return windows() && platformTag() != null;
    }

    private static String platformTag() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (arch.equals("amd64") || arch.equals("x86_64")) return "win64";
        if (arch.equals("aarch64") || arch.equals("arm64")) return "winarm64";
        return null;
    }

    /** Asset found in the latest release (name, size in bytes, url, sha256). */
    public record Asset(String name, long size, String url, String sha256) {}

    private static HttpClient http() {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();
    }

    /** Looks up the newest release build (blocking, call off the render thread). */
    public static Asset lookup() throws IOException, InterruptedException {
        HttpClient c = http();
        HttpResponse<String> r = c.send(HttpRequest.newBuilder(URI.create(RELEASE_API))
                .header("Accept", "application/vnd.github+json").header("User-Agent", "VibeyClient")
                .timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new IOException("GitHub answered " + r.statusCode());
        JsonObject rel = JsonParser.parseString(r.body()).getAsJsonObject();
        JsonArray assets = rel.getAsJsonArray("assets");
        Pattern p = Pattern.compile(String.format(Locale.ROOT, ASSET.pattern(), platformTag()));
        JsonObject best = null;
        int bestVer = -1;
        String sums = null;
        for (JsonElement e : assets) {
            JsonObject a = e.getAsJsonObject();
            String name = a.get("name").getAsString();
            if (name.equals("checksums.sha256")) sums = a.get("browser_download_url").getAsString();
            Matcher m = p.matcher(name);
            if (m.matches()) {
                int ver = Integer.parseInt(m.group(1)) * 1000 + Integer.parseInt(m.group(2));
                if (ver > bestVer) {
                    bestVer = ver;
                    best = a;
                }
            }
        }
        if (best == null || sums == null) throw new IOException("No matching FFmpeg build in the latest release");
        String name = best.get("name").getAsString();
        HttpResponse<String> s = c.send(HttpRequest.newBuilder(URI.create(sums)).header("User-Agent", "VibeyClient")
                .timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString());
        String sha = null;
        for (String line : s.body().split("\n")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 2 && parts[1].replace("*", "").equals(name)) sha = parts[0].toLowerCase(Locale.ROOT);
        }
        if (sha == null) throw new IOException("No checksum for " + name);
        return new Asset(name, best.get("size").getAsLong(), best.get("browser_download_url").getAsString(), sha);
    }

    /** Downloads, verifies and unpacks {@code asset} into {@link #ownDir()} (blocking). {@code progress}: 0..1. */
    public static void install(Asset asset, Consumer<Float> progress) throws Exception {
        Path dir = ownDir();
        Files.createDirectories(dir);
        Path zip = dir.resolve("download.zip.part");
        HttpResponse<InputStream> r = http().send(HttpRequest.newBuilder(URI.create(asset.url())).header("User-Agent", "VibeyClient")
                .timeout(Duration.ofMinutes(20)).build(), HttpResponse.BodyHandlers.ofInputStream());
        if (r.statusCode() != 200) throw new IOException("Download answered " + r.statusCode());
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        long done = 0;
        try (InputStream in = r.body(); OutputStream out = Files.newOutputStream(zip)) {
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                sha.update(buf, 0, n);
                done += n;
                progress.accept(Math.min(0.99f, done / (float) Math.max(1, asset.size())));
            }
        }
        String got = HexFormat.of().formatHex(sha.digest());
        if (!got.equalsIgnoreCase(asset.sha256())) {
            Files.deleteIfExists(zip);
            throw new IOException("Checksum mismatch");
        }
        Path bin = dir.resolve("bin");
        Path staging = dir.resolve("bin.new");
        deleteTree(staging);
        Files.createDirectories(staging);
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                String name = e.getName().replace('\\', '/');
                int slash = name.indexOf('/');
                String rel = slash >= 0 ? name.substring(slash + 1) : name;
                Path target;
                if (rel.startsWith("bin/") && !e.isDirectory() && rel.indexOf('/', 4) < 0) target = staging.resolve(rel.substring(4));
                else if (rel.equals("LICENSE.txt")) target = dir.resolve("LICENSE.txt");
                else continue;
                if (!target.normalize().startsWith(dir.normalize())) continue;
                Files.copy(zin, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.writeString(dir.resolve("SOURCE.txt"), "FFmpeg " + asset.name() + "\nfrom https://" + SOURCE
                + " (license " + LICENSE + ", see LICENSE.txt)\nsha256 " + asset.sha256() + "\n", StandardCharsets.UTF_8);
        deleteTree(bin);
        Files.move(staging, bin);
        Files.deleteIfExists(zip);
        progress.accept(1f);
        refresh();
        Vibey.LOG.info("Installed FFmpeg {} to {}", asset.name(), bin);
    }

    private static void deleteTree(Path p) throws IOException {
        if (!Files.exists(p)) return;
        try (var s = Files.walk(p)) {
            for (Path q : s.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(q);
        }
    }
}
