package dev.henny.cosmetico.mc.preview;

import dev.henny.cosmetico.mc.render.AvatarCarrier;
import dev.henny.cosmetico.preview.PreviewState;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Quaternionf;
import org.joml.Vector3f;
//? if >=1.21.9 {
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerSkin;
//?} else if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.PlayerSkin;
*///?} else
/*import net.minecraft.client.resources.PlayerSkin;*/

final class PreviewRenderer {
    private final PreviewState state;
    private final PreviewSkin skins = new PreviewSkin();
    //? if >=1.21.9 {
    private final AvatarRenderState rs = new AvatarRenderState();
    //?} else if >=1.21.2
    /*private final PlayerRenderState rs = new PlayerRenderState();*/

    PreviewRenderer(PreviewState state) {
        this.state = state;
    }

    //? if >=26.1 {
    /*void render(GuiGraphicsExtractor g, int x, int y, int w, int h, float size, double now) {
    *///?} else
    void render(GuiGraphics g, int x, int y, int w, int h, float size, double now) {
        PlayerSkin skin = skins.skin(state.avatar);
        //? if >=1.21.2 {
        rs.skin = skin;
        rs.bodyRot = 180f + state.yaw();
        rs.yRot = 0f;
        rs.xRot = 0f;
        rs.ageInTicks = (float) (now * 20.0);
        rs.walkAnimationPos = 0f;
        rs.walkAnimationSpeed = 0f;
        rs.scale = 1f;
        rs.boundingBoxWidth = 0.6f;
        rs.boundingBoxHeight = 1.8f;
        rs.isInvisible = false;
        rs.showCape = true;
        rs.capeFlap = 0f;
        rs.capeLean = 0f;
        rs.capeLean2 = 0f;
        rs.nameTag = null;
        ((AvatarCarrier) rs).cosmetico$setAvatar(state.avatar);
        //?}
        //? if >=1.21.9 {
        rs.lightCoords = 0xF000F0;
        rs.outlineColor = 0;
        rs.shadowPieces.clear();
        //?}
        //? if >=1.21.6 {
        //? if >=26.2 {
        /*rs.entityType = net.minecraft.world.entity.EntityTypes.PLAYER;
        *///?} else
        rs.entityType = net.minecraft.world.entity.EntityType.PLAYER;
        Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cam = new Quaternionf().rotateX(0f);
        rot.mul(cam);
        Vector3f translate = new Vector3f(0f, 1.8f / 2f + 0.06f, 0f);
        //? if >=26.1 {
        /*g.entity(rs, size, translate, rot, cam, x, y, x + w, y + h);
        *///?} else
        g.submitEntityRenderState(rs, size, translate, rot, cam, x, y, x + w, y + h);
        //?} else if >=1.21.2 {
        /*PreviewLegacy.render(g, x, y, w, h, size, rs, skin);
        *///?} else {
        /*PreviewLegacy.render(g, x, y, w, h, size, state.yaw(), state.avatar, skin, now);
        *///?}
    }
}
