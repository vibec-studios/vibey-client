package dev.henny.vibeyclient.module.screens.apply;

import dev.henny.vibeyclient.module.screens.core.Box;

public final class WidgetState {
    Box base;
    Box applied;
    float scale = 1f;
    int logicalW;
    int logicalH;
    boolean hidden;
    boolean hiddenByUs;
    boolean visibleBefore = true;

    public boolean plain() {
        return !hidden && scale == 1f;
    }

    public boolean hidden() {
        return hidden;
    }

    public float scale() {
        return scale;
    }

    public int logicalW() {
        return logicalW;
    }

    public int logicalH() {
        return logicalH;
    }
}
