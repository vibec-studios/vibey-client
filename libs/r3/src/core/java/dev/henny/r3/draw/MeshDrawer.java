package dev.henny.r3.draw;

import dev.henny.r3.math.Argb;
import dev.henny.r3.object.Mesh;
import dev.henny.r3.object.Transform;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.render.WorldCanvas;
import org.joml.Vector3dc;

public final class MeshDrawer {
    public void draw(WorldCanvas canvas, WorldObject object, Mesh mesh, int light) {
        Transform t = object.transform();
        Vector3dc cam = canvas.cameraPosition();
        float alpha = object.state().alpha();
        canvas.push();
        canvas.translate((float) (t.position().x - cam.x()), (float) (t.position().y - cam.y()), (float) (t.position().z - cam.z()));
        canvas.rotate(t.orientation());
        canvas.scale(t.scale(), t.scale(), t.scale());
        canvas.beginQuads(mesh.mode(), mesh.texture());
        for (int i = 0; i < mesh.vertexCount(); i++) {
            canvas.vertex(mesh.x(i), mesh.y(i), mesh.z(i), mesh.u(i), mesh.v(i), Argb.multiplyAlpha(mesh.color(i), alpha), light);
        }
        canvas.endQuads();
        canvas.pop();
    }
}
