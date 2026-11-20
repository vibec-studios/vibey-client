package dev.henny.vibeyclient.module.waypoints.core.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class TileCachePolicy {
    private TileCachePolicy() {}

    public static long total(List<CacheFile> files) {
        long sum = 0;
        for (CacheFile f : files) sum += f.bytes();
        return sum;
    }

    public static List<CacheFile> evict(List<CacheFile> files, long maxBytes, Set<String> keep) {
        long total = total(files);
        List<CacheFile> out = new ArrayList<>();
        if (total <= maxBytes) return out;
        List<CacheFile> sorted = new ArrayList<>(files);
        sorted.sort(Comparator.comparingLong(CacheFile::lastUsed).thenComparing(CacheFile::path));
        for (CacheFile f : sorted) {
            if (total <= maxBytes) break;
            if (keep.contains(f.path())) continue;
            out.add(f);
            total -= f.bytes();
        }
        return out;
    }
}
