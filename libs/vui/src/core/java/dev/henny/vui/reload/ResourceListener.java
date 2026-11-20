package dev.henny.vui.reload;

@FunctionalInterface
public interface ResourceListener {
    boolean changed(String relativePath);
}
