package dev.henny.vui.dev;

public sealed interface PreviewStep {
    record Hover(String target) implements PreviewStep {
    }

    record Scroll(float lines) implements PreviewStep {
    }

    record Click(String target) implements PreviewStep {
    }

    record Wait(float seconds) implements PreviewStep {
    }

    static PreviewStep hover(String target) {
        if (target.isBlank()) throw new IllegalArgumentException("--hover needs x,y (GUI px), an id or a selector");
        return new Hover(target.trim());
    }
}
