package dev.henny.companio.pack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class PackHash {
    private PackHash() {}

    public static String of(PackFiles files) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            List<String> paths = new ArrayList<>(files.list());
            paths.sort(null);
            for (String p : paths) {
                md.update(p.getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
                md.update(files.read(p));
                md.update((byte) 0);
            }
            return hex(md.digest()).substring(0, 16);
        } catch (NoSuchAlgorithmException | IOException e) {
            return "0000000000000000";
        }
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(Character.forDigit((x >> 4) & 15, 16)).append(Character.forDigit(x & 15, 16));
        return sb.toString();
    }
}
