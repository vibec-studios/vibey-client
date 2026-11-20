package dev.henny.vibeyclient.media.ffmpeg;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FfmpegRelease {
    private static final String RELEASE_API = "https://api.github.com/repos/BtbN/FFmpeg-Builds/releases/latest";
    private static final String LATEST_DOWNLOAD = "https://github.com/BtbN/FFmpeg-Builds/releases/latest/download/";
    private static final Pattern ASSET = Pattern.compile("ffmpeg-n(\\d+)\\.(\\d+)-latest-%s-lgpl-shared-\\d+\\.\\d+\\.zip");
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private FfmpegRelease() {}

    static HttpClient http() {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();
    }

    public static FfmpegAsset lookup() throws IOException, InterruptedException {
        HttpClient c = http();
        HttpResponse<String> r = c.send(HttpRequest.newBuilder(URI.create(RELEASE_API))
                .header("Accept", "application/vnd.github+json").header("User-Agent", "VibeyClient")
                .timeout(TIMEOUT).build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) {
            Vibey.LOG.info("FFmpeg lookup: GitHub API answered {}, using the release's checksum list", r.statusCode());
            return lookupFromChecksums(c);
        }
        return fromApi(c, JsonParser.parseString(r.body()).getAsJsonObject());
    }

    private static FfmpegAsset fromApi(HttpClient c, JsonObject release) throws IOException, InterruptedException {
        JsonArray assets = release.getAsJsonArray("assets");
        Pattern p = Pattern.compile(String.format(Locale.ROOT, ASSET.pattern(), FfmpegPlatform.tag()));
        JsonObject best = null;
        int bestVer = -1;
        String sums = null;
        for (JsonElement e : assets) {
            JsonObject a = e.getAsJsonObject();
            String name = a.get("name").getAsString();
            if (name.equals("checksums.sha256")) sums = a.get("browser_download_url").getAsString();
            Matcher m = p.matcher(name);
            if (!m.matches()) continue;
            int ver = version(m);
            if (ver > bestVer) {
                bestVer = ver;
                best = a;
            }
        }
        if (best == null || sums == null) throw new IOException("No matching FFmpeg build in the latest release");
        String name = best.get("name").getAsString();
        HttpResponse<String> s = c.send(HttpRequest.newBuilder(URI.create(sums)).header("User-Agent", "VibeyClient")
                .timeout(TIMEOUT).build(), HttpResponse.BodyHandlers.ofString());
        String sha = null;
        for (String line : s.body().split("\n")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 2 && parts[1].replace("*", "").equals(name)) sha = parts[0].toLowerCase(Locale.ROOT);
        }
        if (sha == null) throw new IOException("No checksum for " + name);
        return new FfmpegAsset(name, best.get("size").getAsLong(), best.get("browser_download_url").getAsString(), sha);
    }

    static FfmpegAsset lookupFromChecksums(HttpClient c) throws IOException, InterruptedException {
        HttpResponse<String> s = c.send(HttpRequest.newBuilder(URI.create(LATEST_DOWNLOAD + "checksums.sha256"))
                .header("User-Agent", "VibeyClient").timeout(TIMEOUT).build(), HttpResponse.BodyHandlers.ofString());
        if (s.statusCode() != 200) throw new IOException("GitHub answered " + s.statusCode());
        FfmpegAsset a = pickFromChecksums(s.body(), FfmpegPlatform.tag());
        if (a == null) throw new IOException("No matching FFmpeg build in the latest release");
        long size = -1;
        try {
            HttpResponse<Void> h = c.send(HttpRequest.newBuilder(URI.create(a.url())).method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .header("User-Agent", "VibeyClient").timeout(TIMEOUT).build(), HttpResponse.BodyHandlers.discarding());
            size = h.headers().firstValueAsLong("Content-Length").orElse(-1);
        } catch (IOException ignored) {}
        return new FfmpegAsset(a.name(), size > 0 ? size : 85L * 1024 * 1024, a.url(), a.sha256());
    }

    public static FfmpegAsset pickFromChecksums(String sums, String tag) {
        if (tag == null) return null;
        Pattern p = Pattern.compile(String.format(Locale.ROOT, ASSET.pattern(), tag));
        String best = null, sha = null;
        int bestVer = -1;
        for (String line : sums.split("\n")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length != 2) continue;
            String name = parts[1].replace("*", "");
            Matcher m = p.matcher(name);
            if (!m.matches()) continue;
            int ver = version(m);
            if (ver > bestVer) {
                bestVer = ver;
                best = name;
                sha = parts[0].toLowerCase(Locale.ROOT);
            }
        }
        return best == null ? null : new FfmpegAsset(best, -1, LATEST_DOWNLOAD + best, sha);
    }

    private static int version(Matcher m) {
        return Integer.parseInt(m.group(1)) * 1000 + Integer.parseInt(m.group(2));
    }
}
