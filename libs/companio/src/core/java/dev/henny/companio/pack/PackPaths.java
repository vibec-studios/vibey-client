package dev.henny.companio.pack;

import java.util.regex.Pattern;

public final class PackPaths {
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_\\-.]+(/[A-Za-z0-9_\\-.]+)*");

    private PackPaths() {}

    public static boolean valid(String path) {
        if (path == null || path.isEmpty() || path.length() > 200) return false;
        if (!VALID.matcher(path).matches()) return false;
        for (String part : path.split("/")) if (part.equals("..") || part.equals(".")) return false;
        return true;
    }

    public static String fileName(String path) {
        int i = path.lastIndexOf('/');
        String n = i < 0 ? path : path.substring(i + 1);
        int dot = n.lastIndexOf('.');
        return dot < 0 ? n : n.substring(0, dot);
    }
}
