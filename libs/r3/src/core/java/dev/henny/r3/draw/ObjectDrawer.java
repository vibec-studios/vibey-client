package dev.henny.r3.draw;

import dev.henny.r3.fx.Effect;
import dev.henny.r3.fx.EffectContext;
import dev.henny.r3.object.Mesh;
import dev.henny.r3.object.ObjectContent;
import dev.henny.r3.object.VideoSurface;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.panel.PanelContent;
import dev.henny.r3.render.Light;
import dev.henny.r3.render.WorldCanvas;
import dev.henny.r3.text.Text3D;

public final class ObjectDrawer {
    private final PanelDrawer panels = new PanelDrawer();
    private final TextDrawer texts = new TextDrawer();
    private final VideoDrawer videos = new VideoDrawer();
    private final MeshDrawer meshes = new MeshDrawer();

    public void draw(WorldCanvas canvas, WorldObject object, EffectContext effects) {
        ObjectContent content = object.content();
        int light = object.fullBright() ? Light.FULL_BRIGHT : object.state().light();
        if (content instanceof PanelContent panel) panels.draw(canvas, object, panel, light);
        else if (content instanceof VideoSurface video) videos.draw(canvas, object, video, light);
        else if (content instanceof Text3D text) texts.draw(canvas, object, text, light);
        else if (content instanceof Mesh mesh) meshes.draw(canvas, object, mesh, light);
        else if (content instanceof Effect effect) effect.draw(canvas, effects);
    }
}
