package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.GridWorld;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.WorldView;

import java.util.List;

final class Scenes {
    static final V3 FEET = new V3(0.5, 65, 0.5);

    private Scenes() {}

    static Scene of(WorldView w) {
        return of(w, List.of(), List.of());
    }

    static Scene of(WorldView w, List<Scene.Subject> players, List<Scene.Mark> marks) {
        return new Scene(w, FEET.add(0, 1.62, 0), FEET, 0, 40, 1, players, marks, Scene.Sky.NOON);
    }

    static GridWorld village() {
        GridWorld w = GridWorld.flat();
        w.box(8, 65, 6, 14, 70, 12, GridWorld.PLANKS).fill(9, 66, 6, 10, 67, 6, 0).set(11, 71, 9, GridWorld.TORCH);
        w.box(-16, 65, -14, -10, 69, -8, GridWorld.BRICKS).set(-13, 70, -11, GridWorld.TORCH);
        w.fill(-6, 64, 10, 2, 64, 20, GridWorld.WATER_KIND);
        return w;
    }

    static GridWorld closedRoom() {
        return GridWorld.flat().box(-2, 64, -2, 2, 68, 2, GridWorld.STONE);
    }

    static Scene.Subject player(String name, V3 feet) {
        return new Scene.Subject(name, name, () -> feet, 1.8);
    }
}
