package dev.henny.r3.mc.render;

import dev.henny.r3.mc.texture.McTextures;
import dev.henny.r3.mc.texture.WhiteTexture;
import dev.henny.r3.render.Light;
import dev.henny.r3.render.PanelCanvas;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.TextMetrics;
import dev.henny.r3.render.TextureHandle;
import dev.henny.r3.render.WorldCanvas;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?} else
/*import net.minecraft.client.renderer.MultiBufferSource;*/

public final class McWorldCanvas implements WorldCanvas {
    private final Vector3d camera = new Vector3d();
    private final MatrixStack stack = new MatrixStack();
    private final DrawList list = new DrawList();
    private final McPanelCanvas panel = new McPanelCanvas();
    private final Vector3f p = new Vector3f();
    private QuadItem batch;
    private boolean emissive;
    private boolean inPanel;

    public McWorldCanvas begin(Vector3dc cameraPosition, Matrix4fc base) {
        WhiteTexture.ensure();
        camera.set(cameraPosition);
        stack.reset(base);
        list.clear();
        batch = null;
        inPanel = false;
        return this;
    }

    //? if >=1.21.9 {
    public void submit(SubmitNodeCollector out) {
        DrawSink.submit(list, out);
    }
    //?} else {
    /*public void draw(MultiBufferSource buffers) {
        DrawSink.draw(list, buffers);
    }
    *///?}

    public int items() {
        return list.size();
    }

    @Override
    public Vector3dc cameraPosition() {
        return camera;
    }

    @Override
    public TextMetrics textMetrics() {
        return McFontMetrics.INSTANCE;
    }

    @Override
    public void push() {
        stack.push();
    }

    @Override
    public void pop() {
        stack.pop();
    }

    @Override
    public void translate(float x, float y, float z) {
        stack.translate(x, y, z);
    }

    @Override
    public void rotate(Quaternionfc rotation) {
        stack.rotate(rotation);
    }

    @Override
    public void scale(float x, float y, float z) {
        stack.scale(x, y, z);
    }

    @Override
    public void multiply(Matrix4fc matrix) {
        stack.multiply(matrix);
    }

    @Override
    public void beginQuads(RenderMode mode, TextureHandle texture) {
        if (batch != null) throw new IllegalStateException("r3 canvas: nested beginQuads");
        RenderMode drawn = mode == RenderMode.EMISSIVE ? RenderMode.TRANSLUCENT : mode;
        batch = list.quads(drawn, McTextures.id(texture));
        emissive = mode.ignoresLight();
    }

    @Override
    public void vertex(float x, float y, float z, float u, float v, int argb, int light) {
        stack.transform(x, y, z, p);
        batch.add(p.x, p.y, p.z, u, v, argb, emissive ? Light.FULL_BRIGHT : light);
    }

    @Override
    public void endQuads() {
        batch = null;
    }

    @Override
    public void glyph(int codepoint, float x, float y, int argb, int light, boolean seeThrough) {
        text(GlyphCache.of(codepoint), x, y, argb, light, seeThrough);
    }

    @Override
    public void text(String text, float x, float y, int argb, int light, boolean seeThrough) {
        list.text(stack.top(), text, x, y, argb, light, seeThrough ? TextMode.SEE_THROUGH : TextMode.NORMAL, false, false);
    }

    @Override
    public PanelCanvas beginPanel(int pixelWidth, int pixelHeight, int light, float alpha) {
        if (inPanel) throw new IllegalStateException("r3 canvas: nested beginPanel");
        inPanel = true;
        panel.begin(stack.top(), pixelWidth, pixelHeight, light, alpha, list);
        return panel;
    }

    @Override
    public void endPanel() {
        inPanel = false;
    }
}
