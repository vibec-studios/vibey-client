package dev.henny.vibeyclient.media.ffmpeg;

import dev.henny.vibeyclient.Vibey;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class FfmpegInstaller {
    private FfmpegInstaller() {}

    public static void install(FfmpegAsset asset, Consumer<Float> progress) throws Exception {
        Path dir = Ffmpeg.ownDir();
        Files.createDirectories(dir);
        Path zip = dir.resolve("download.zip.part");
        download(asset, zip, progress);
        Path bin = dir.resolve("bin");
        Path staging = dir.resolve("bin.new");
        deleteTree(staging);
        Files.createDirectories(staging);
        unpack(zip, dir, staging);
        Files.writeString(dir.resolve("SOURCE.txt"), "FFmpeg " + asset.name() + "\nfrom https://" + Ffmpeg.SOURCE
                + " (license " + Ffmpeg.LICENSE + ", see LICENSE.txt)\nsha256 " + asset.sha256() + "\n", StandardCharsets.UTF_8);
        deleteTree(bin);
        Files.move(staging, bin);
        Files.deleteIfExists(zip);
        progress.accept(1f);
        Ffmpeg.refresh();
        Vibey.LOG.info("Installed FFmpeg {} to {}", asset.name(), bin);
    }

    private static void download(FfmpegAsset asset, Path zip, Consumer<Float> progress) throws Exception {
        HttpResponse<InputStream> r = FfmpegRelease.http().send(HttpRequest.newBuilder(URI.create(asset.url()))
                .header("User-Agent", "VibeyClient").timeout(Duration.ofMinutes(20)).build(), HttpResponse.BodyHandlers.ofInputStream());
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
        if (!HexFormat.of().formatHex(sha.digest()).equalsIgnoreCase(asset.sha256())) {
            Files.deleteIfExists(zip);
            throw new IOException("Checksum mismatch");
        }
    }

    private static void unpack(Path zip, Path dir, Path staging) throws IOException {
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
    }

    private static void deleteTree(Path p) throws IOException {
        if (!Files.exists(p)) return;
        try (var s = Files.walk(p)) {
            for (Path q : s.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(q);
        }
    }
}
