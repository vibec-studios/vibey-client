package dev.henny.vibeyclient.module.servers.core;

import java.util.List;

public final class ListMove {
    private ListMove() {}

    public static <T> void move(List<T> list, int from, int to) {
        if (from == to) return;
        list.add(to, list.remove(from));
    }

    public static void bySwaps(int from, int to, Swapper swapper) {
        int step = to > from ? 1 : -1;
        for (int i = from; i != to; i += step) swapper.swap(i, i + step);
    }

    public interface Swapper {
        void swap(int a, int b);
    }
}
