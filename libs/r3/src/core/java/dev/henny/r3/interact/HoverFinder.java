package dev.henny.r3.interact;

import dev.henny.r3.math.OrientedRect;
import dev.henny.r3.math.RectHit;
import dev.henny.r3.panel.PointerContent;

public final class HoverFinder {
    private final OrientedRect rect = new OrientedRect();
    private final RectHit hit = new RectHit();

    public boolean find(Pointer pointer, Iterable<? extends Interactable> candidates, InteractionConfig config, HoverState out) {
        out.clear();
        double best = pointer.maxDistance();
        for (Interactable target : candidates) {
            if (!target.hoverable()) continue;
            rect.set(target.transform());
            double border = bordersActive(target)
                    ? BorderZone.width(pointer.ray().origin().distance(rect.center()), rect.halfWidth(), rect.halfHeight(), config)
                    : 0;
            double margin = BorderZone.outside(border, config);
            if (!rect.hit(pointer.ray(), margin, target.rules().doubleSided(), best, hit)) continue;
            best = hit.distance();
            classify(target, BorderZone.inside(border, config), border > 0, out);
        }
        return out.target() != null;
    }

    private void classify(Interactable target, double inside, boolean borders, HoverState out) {
        String control = null;
        PointerContent content = target.pointerContent();
        if (content != null && hit.inside()) {
            control = content.controlAt(hit.pixelX(target.pixelWidth()), hit.pixelY(target.pixelHeight()));
        }
        ResizeHandle handle = borders
                ? BorderZone.handleAt(hit.localX(), hit.localY(), rect.halfWidth(), rect.halfHeight(), inside)
                : ResizeHandle.NONE;
        if (control != null) {
            out.set(target, HoverZone.CONTROL, ResizeHandle.NONE, control, hit);
        } else if (handle != ResizeHandle.NONE) {
            out.set(target, handle.corner() ? HoverZone.CORNER : HoverZone.EDGE, handle, null, hit);
        } else {
            out.set(target, HoverZone.BODY, ResizeHandle.NONE, null, hit);
        }
    }

    private static boolean bordersActive(Interactable target) {
        return !target.locked() && target.rules().edgeResizable();
    }
}
