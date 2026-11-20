package dev.henny.r3.vui;

import dev.henny.r3.input.InputCode;
import dev.henny.r3.interact.InteractionEventKind;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.panel.PointerButton;
import dev.henny.r3.support.ImagePanelCanvas;
import dev.henny.r3.support.Rig;
import dev.henny.r3.view.ViewFrustum;
import dev.henny.vui.dom.Element;
import dev.henny.vui.headless.Headless;
import dev.henny.vui.headless.ImageCanvas;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VuiPanelContentTest {
    final Headless headless = Headless.create();
    final DemoDocs.Controller controller = new DemoDocs.Controller();
    final VuiPanelContent content = new VuiPanelContent(
            headless.vui().fromSource("player.vui", DemoDocs.PLAYER, controller), TextureResolver.NONE);

    VuiPanelContentTest() {
        content.layout(128, 72);
        content.update(0f);
    }

    float[] center(String id) {
        Element e = content.document().find(id);
        return new float[] {e.box().x + e.box().width / 2f, e.box().y + e.box().height / 2f};
    }

    @Test
    void controlsAreElementsWithAnIdOrAClickHandler() {
        float[] play = center("play");
        assertEquals("play", content.controlAt(play[0], play[1]));
        Element stop = content.document().query("button").get(2);
        String stopId = content.controlAt(stop.box().x + 2, stop.box().y + 2);
        assertTrue(stopId.startsWith(VuiControls.ANONYMOUS_PREFIX), stopId);
        Element title = content.document().query(".title").get(0);
        assertNull(content.controlAt(title.box().x + 1, title.box().y + 1));
    }

    @Test
    void pointerDownAndUpClickTheButton() {
        float[] next = center("next");
        content.pointerMove(next[0], next[1]);
        assertTrue(content.pointerDown(next[0], next[1], PointerButton.PRIMARY));
        content.pointerUp(next[0], next[1], PointerButton.PRIMARY);
        assertEquals(1, controller.nexts);
    }

    @Test
    void wheelOutsideAScrollContainerIsNotConsumed() {
        float[] play = center("play");
        assertFalse(content.scroll(play[0], play[1], 1.0));
    }

    @Test
    void hoverChangesTheButtonColor() {
        float[] play = center("play");
        Element button = content.document().find("play");
        float[] edge = {button.box().x + 1.5f, button.box().y + 1.5f};
        int before = pixel(edge);
        content.pointerMove(play[0], play[1]);
        for (int i = 0; i < 20; i++) content.update(0.05f);
        assertNotEquals(before, pixel(edge));
    }

    @Test
    void lookAtPlayAndLeftClickInTheScene() {
        Rig placed = new Rig();
        WorldObject screen = placed.wallScreen();
        screen.content(content);
        placed.scene.update(placed.camera, ViewFrustum.ALL, Rig.DT);
        float[] play = center("play");
        float u = play[0] / screen.pixelWidth() - 0.5f;
        float v = 0.5f - play[1] / screen.pixelHeight();
        double x = screen.transform().position().x() - u * screen.transform().worldWidth();
        double y = screen.transform().position().y() + v * screen.transform().worldHeight();
        placed.lookAt(x, y, screen.transform().position().z()).frame();
        assertEquals("play", placed.interaction.hover().controlId());
        placed.click(InputCode.MOUSE_LEFT);
        assertEquals(1, controller.plays);
        assertEquals("play", placed.last().controlId());
        assertEquals(1, placed.count(InteractionEventKind.ACTION));
    }

    int pixel(float[] at) {
        ImageCanvas image = new ImageCanvas(128, 72, 1, headless.textures(), Headless.BACKGROUND);
        content.draw(new ImagePanelCanvas(image, 128, 72));
        return image.pixel((int) at[0], (int) at[1]);
    }
}
