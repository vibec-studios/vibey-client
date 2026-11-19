package dev.henny.cosmetico.preview;

import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.sim.PetGeometry;
import dev.henny.cosmetico.state.Avatar;

public final class PreviewLayout {
    private PreviewLayout() {}

    public static void placePet(Avatar a, Bundle pet) {
        double ang = Math.toRadians(a.frame.bodyYaw);
        double rxw = -Math.cos(ang), rzw = -Math.sin(ang), fx = -Math.sin(ang), fz = Math.cos(ang);
        double side = PetGeometry.side(pet);
        a.pet.place(rxw * side - fx * 0.2, 0, rzw * side - fz * 0.2, a.frame.bodyYaw);
        a.pet.move = 0;
    }

    public static float span(Bundle pet) {
        if (pet == null) return 1.6f;
        return (float) Math.max(3.1, 2 * (PetGeometry.side(pet) + PetGeometry.halfWidth(pet)) + 0.3);
    }

    public static float size(int w, int h, float span) {
        return Math.min(h / 2.3f, w / span);
    }
}
