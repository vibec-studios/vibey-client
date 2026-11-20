package dev.henny.vui.bind;

import java.util.function.Supplier;

public final class Tracker {
    private static Effect current;

    private Tracker() {
    }

    static void track(Observable observable) {
        if (current != null) current.depend(observable);
    }

    public static void untrackedRead() {
        if (current != null) current.markUntracked();
    }

    static Effect swap(Effect next) {
        Effect previous = current;
        current = next;
        return previous;
    }

    public static <T> T untracked(Supplier<T> body) {
        Effect previous = swap(null);
        try {
            return body.get();
        } finally {
            swap(previous);
        }
    }

    public static boolean isTracking() {
        return current != null;
    }
}
