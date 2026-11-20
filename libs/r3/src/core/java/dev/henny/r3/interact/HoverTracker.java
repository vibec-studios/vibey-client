package dev.henny.r3.interact;

import dev.henny.r3.math.OrientedRect;
import dev.henny.r3.math.Ray;
import dev.henny.r3.math.RectHit;
import dev.henny.r3.panel.PointerContent;

import java.util.Objects;

public final class HoverTracker {
    private final OrientedRect rect = new OrientedRect();
    private final RectHit hit = new RectHit();
    private Interactable current;
    private String control;
    private float lastX;
    private float lastY;

    public void update(HoverState hover, boolean frozen) {
        Interactable target = !frozen && hover.zone().onContent() ? hover.target() : null;
        if (target != current) {
            exit();
            current = target;
        }
        PointerContent content = current == null ? null : current.pointerContent();
        if (content == null) return;
        content.pointerMove(hover.pixelX(), hover.pixelY());
        if (!Objects.equals(control, hover.controlId())) {
            control = hover.controlId();
            content.hoverChanged(control);
        }
    }

    public boolean dragMove(Interactable target, Ray ray) {
        PointerContent content = target.pointerContent();
        if (content == null || !rect.set(target.transform()).planeHit(ray, hit)) return false;
        lastX = hit.pixelX(target.pixelWidth());
        lastY = hit.pixelY(target.pixelHeight());
        content.pointerMove(lastX, lastY);
        return true;
    }

    public void pressedAt(float x, float y) {
        lastX = x;
        lastY = y;
    }

    public float lastX() {
        return lastX;
    }

    public float lastY() {
        return lastY;
    }

    public void exit() {
        PointerContent content = current == null ? null : current.pointerContent();
        if (content != null) {
            if (control != null) content.hoverChanged(null);
            content.pointerExit();
        }
        current = null;
        control = null;
    }

    public void forget(Interactable target) {
        if (current == target) {
            current = null;
            control = null;
        }
    }
}
