package dev.henny.vibeyclient.module.media.video.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
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
import java.util.function.Consumer;

/**
 * Downloads the official MCEF Modern jar for this Minecraft version from Modrinth into {@code mods/} (checked with
 * Modrinth's SHA-512) – only after the user agreed in the setup prompt. Takes effect after a restart.
 */
public final class McefInstaller {
    private static final String API = "https://api.modrinth.com/v2/project/mcef-modern/version";

    /** The jar was put into mods/ in this session: it works after a restart. */
    public static volatile boolean installedThisSession;

    public record Release(String version, String fileName, long size, String url, String sha512) {}

    private McefInstaller() {}

    private static HttpClient http() {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();
    }

    private static String userAgent() {
        return "vibec-studios/vibey-client/" + BuildInfo.VERSION;
    }

    /** Newest release for Fabric + this game version (blocking). */
    public static Release lookup() throws IOException, InterruptedException {
        String q = "?loaders=" + URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8)
                + "&game_versions=" + URLEncoder.encode("[\"" + BuildInfo.MINECRAFT + "\"]", StandardCharsets.UTF_8);
        HttpResponse<String> r = http().send(HttpRequest.newBuilder(URI.create(API + q)).header("User-Agent", userAgent())
                .timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new IOException("Modrinth answered " + r.statusCode());
        JsonArray versions = JsonParser.parseString(r.body()).getAsJsonArray();
        for (JsonElement ve : versions) {
            JsonObject v = ve.getAsJsonObject();
            if (!"release".equals(v.get("version_type").getAsString())) continue;
            JsonObject pick = null;
            for (JsonElement fe : v.getAsJsonArray("files")) {
                JsonObject f = fe.getAsJsonObject();
                String name = f.get("filename").getAsString();
                if (name.endsWith("-sources.jar") || !name.endsWith(".jar")) continue;
                if (pick == null || f.get("primary").getAsBoolean()) pick = f;
            }
            if (pick == null) continue;
            return new Release(v.get("version_number").getAsString(), pick.get("filename").getAsString(), pick.get("size").getAsLong(),
                    pick.get("url").getAsString(), pick.getAsJsonObject("hashes").get("sha512").getAsString());
        }
        throw new IOException("No MCEF Modern release for " + BuildInfo.MINECRAFT);
    }

    public static Path modsDir() {
        return FabricLoader.getInstance().getGameDir().resolve("mods");
    }

    /** Downloads and verifies the jar into mods/ (blocking). Returns the installed file. */
    public static Path install(Release rel, Consumer<Float> progress) throws Exception {
        if (!rel.url().startsWith("https://cdn.modrinth.com/")) throw new IOException("Unexpected download host");
        String safeName = Path.of(rel.fileName()).getFileName().toString();
        if (!safeName.endsWith(".jar")) throw new IOException("Unexpected file " + safeName);
        Path mods = modsDir();
        Files.createDirectories(mods);
        Path part = mods.resolve(safeName + ".part");
        HttpResponse<InputStream> r = http().send(HttpRequest.newBuilder(URI.create(rel.url())).header("User-Agent", userAgent())
                .timeout(Duration.ofMinutes(5)).build(), HttpResponse.BodyHandlers.ofInputStream());
        if (r.statusCode() != 200) throw new IOException("Download answered " + r.statusCode());
        MessageDigest sha = MessageDigest.getInstance("SHA-512");
        long done = 0;
        try (InputStream in = r.body(); OutputStream out = Files.newOutputStream(part)) {
            byte[] buf = new byte[1 << 15];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                sha.update(buf, 0, n);
                done += n;
                progress.accept(Math.min(0.99f, done / (float) Math.max(1, rel.size())));
            }
        }
        if (!HexFormat.of().formatHex(sha.digest()).equalsIgnoreCase(rel.sha512())) {
            Files.deleteIfExists(part);
            throw new IOException("Checksum mismatch");
        }
        Path jar = mods.resolve(safeName);
        Files.move(part, jar, StandardCopyOption.REPLACE_EXISTING);
        installedThisSession = true;
        progress.accept(1f);
        Vibey.LOG.info("Installed {} {} to {} (restart needed)", Web.MOD_NAME, rel.version(), jar);
        return jar;
    }
}
