package dev.henny.r3.scene;

import dev.henny.r3.object.Visibility;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.support.RecordingCanvas;
import dev.henny.r3.support.Rig;
import dev.henny.r3.text.Text3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LayerAndOpacityTest {
    @Test
    void opacityScalesTheFade() {
        Visibility v = new Visibility().maxDistance(10).fadeDistance(4).opacity(0.5f);
        assertEquals(0.5f, v.alphaAt(3), 1e-6);
        assertEquals(0.25f, v.alphaAt(8), 1e-6);
        assertEquals(0f, v.alphaAt(11), 1e-6);
        assertEquals(0f, v.hidden(true).alphaAt(1), 1e-6);
    }

    @Test
    void lowerLayersDrawFirst() {
        Rig rig = new Rig();
        WorldObject sky = rig.scene.add(WorldObject.create("sky").content(new Text3D("sky")).layer(-1000));
        sky.transform().position(0.5, 65, 2);
        WorldObject text = rig.scene.add(WorldObject.create("text").content(new Text3D("words")));
        text.transform().position(0.5, 65, 4);
        rig.frame();
        rig.scene.render(new RecordingCanvas());
        assertSame(sky, rig.scene.visibleObjects().get(0));
    }
}
