package dev.henny.vibeyclient.module.screens.apply;

public interface StateHolder {
    WidgetState vibey$screens();

    void vibey$screens(WidgetState state);

    void vibey$size(int width, int height);
}
