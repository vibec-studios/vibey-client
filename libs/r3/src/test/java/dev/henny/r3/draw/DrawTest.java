package dev.henny.r3.draw;

import dev.henny.r3.math.Axes;
import dev.henny.r3.object.Mesh;
import dev.henny.r3.object.Transform;
import dev.henny.r3.object.VideoSurface;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.TextureHandle;
import dev.henny.r3.support.RecordingCanvas;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawTest {
    private final RecordingCanvas canvas = new RecordingCanvas();

    private WorldObject screen() {
        WorldObject o = new WorldObject("v", "test:video");
        o.transform().size(2f, 1f).position(0, 64, 5);
        Axes.facing(0, 0, -1, o.transform().orientation());
        o.state().update(true, true, 5, 1f, 0xF000F0);
        canvas.camera.set(0, 64, 0);
        return o;
    }

    @Test
    void panelPixelsMapOntoTheRectangle() {
        Transform t = screen().transform();
        Matrix4f m = new PanelMatrix().compute(t, 200, 100, new Vector3d(0, 64, 0), new Matrix4f());
        Vector3f topLeft = m.transformPosition(new Vector3f(0, 0, 0));
        Vector3f bottomRight = m.transformPosition(new Vector3f(200, 100, 0));
        assertEquals(1, topLeft.x, 1e-5);
        assertEquals(0.5, topLeft.y, 1e-5);
        assertEquals(5, topLeft.z, 1e-5);
        assertEquals(-1, bottomRight.x, 1e-5);
        assertEquals(-0.5, bottomRight.y, 1e-5);
        Vector3f lifted = m.transformPosition(new Vector3f(100, 50, 10));
        assertEquals(4.9, lifted.z, 1e-5);
    }

    @Test
    void videoWithPictureDrawsPlateAndPicture() {
        WorldObject o = screen();
        VideoSurface v = new VideoSurface().texture(TextureHandle.resource("test:frame", 1280, 720));
        new VideoDrawer().draw(canvas, o, v, 0xF000F0);
        assertEquals(12, canvas.vertices);
        assertEquals(1, v.drawnFrames());
        assertEquals(16f / 9f, v.aspect(), 1e-5);
    }

    @Test
    void videoWithoutPictureShowsTheStatus() {
        WorldObject o = screen();
        VideoSurface v = new VideoSurface().status("Alex shared youtube.com - press N to load");
        new VideoDrawer().draw(canvas, o, v, 0xF000F0);
        assertEquals(12, canvas.vertices);
        assertTrue(canvas.texts.size() >= 1 && canvas.texts.size() <= 3);
        assertEquals("Alex shared youtube.com - press N to load", String.join(" ", canvas.texts));
        assertEquals(0, canvas.depth);
    }

    @Test
    void meshesDrawTheirQuads() {
        WorldObject o = screen();
        Mesh box = Mesh.builder(RenderMode.CUTOUT, null).box(0.5f, 0.5f, 0.5f, -1).build();
        assertEquals(24, box.vertexCount());
        new MeshDrawer().draw(canvas, o, box, 0);
        assertEquals(24, canvas.vertices);
        assertEquals(Math.sqrt(0.75), box.boundingRadius(new Transform()), 1e-6);
        assertThrows(IllegalArgumentException.class,
                () -> Mesh.builder(RenderMode.CUTOUT, null).vertex(0, 0, 0, 0, 0, -1).build());
    }
}
