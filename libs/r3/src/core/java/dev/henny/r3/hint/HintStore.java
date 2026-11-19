package dev.henny.r3.hint;

public interface HintStore {
    boolean seen(String hintId);

    void markSeen(String hintId);
}
