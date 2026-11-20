package dev.henny.r3.vui;

import dev.henny.r3.anchor.FaceSnap;
import dev.henny.r3.input.InputCode;
import dev.henny.r3.math.Face;
import dev.henny.r3.panel.PointerButton;
import dev.henny.r3.support.Rig;
import dev.henny.r3.object.WorldObject;
import dev.henny.vui.dom.Element;
import dev.henny.vui.headless.Headless;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VuiKeysTest {
    static final String DOC = """
            <template>
              <column gap="4">
                <textfield id="url" v-model="url" @submit="go()"/>
                <button id="ok" @click="go()">OK</button>
              </column>
            </template>
            """;

    public static final class Form {
        public String url = "";
        public int submits;

        public void go() {
            submits++;
        }
    }

    final Headless headless = Headless.create();
    final Form form = new Form();
    final VuiPanelContent content = new VuiPanelContent(headless.vui().fromSource("form.vui", DOC, form), TextureResolver.NONE);

    VuiKeysTest() {
        content.layout(160, 60);
        content.update(0f);
    }

    void click(String id) {
        Element e = content.document().find(id);
        float x = e.box().x + 2, y = e.box().y + 2;
        content.pointerDown(x, y, PointerButton.PRIMARY);
        content.pointerUp(x, y, PointerButton.PRIMARY);
        content.update(0f);
    }

    @Test
    void clickedButtonDropsFocus() {
        click("ok");
        assertEquals(1, form.submits);
        assertNull(content.document().events().focus().focused(), "no focus ring left on the button");
        assertFalse(content.keyFocused());
    }

    @Test
    void clickedTextFieldTakesKeysUntilReleased() {
        click("url");
        assertTrue(content.keyFocused());
        for (char c : "https://x.y".toCharArray()) content.charTyped(c, 0);
        content.update(0f);
        assertEquals("https://x.y", form.url);
        content.keyDown(257, 0, 0, false);
        assertEquals(1, form.submits, "Enter submits");
        content.releaseKeyFocus();
        assertFalse(content.keyFocused());
    }

    @Test
    void sceneFindsTheFocusedPanel() {
        Rig rig = new Rig();
        WorldObject o = rig.scene.add(WorldObject.create("test:form").content(content));
        assertNull(rig.scene.keyTarget());
        content.focus("url");
        assertSame(o, rig.scene.keyTarget());
        rig.scene.releaseKeys();
        assertNull(rig.scene.keyTarget());
    }

    @Test
    void pressAndReleaseInTheSameFrameStillClick() {
        Rig rig = new Rig();
        WorldObject o = WorldObject.create("test:form").content(content);
        o.dimension(rig.world.dimension);
        o.transform().size(2.5f, 1f).position(0.5, 65, 5 - FaceSnap.DEFAULT_OFFSET);
        FaceSnap.orientation(Face.NORTH, Face.UP, o.transform().orientation());
        o.pixelsPerBlock(64f);
        rig.scene.add(o);
        Element ok = content.document().find("ok");
        rig.frame();
        double u = (ok.box().x + ok.box().width / 2.0) / o.pixelWidth();
        double v = (ok.box().y + ok.box().height / 2.0) / o.pixelHeight();
        rig.lookAt(0.5 + (u - 0.5) * 2.5, 65 + (0.5 - v), 5).frames(2);
        assertEquals("ok", rig.interaction.hover().controlId());
        rig.press(InputCode.mouse(0));
        rig.release(InputCode.mouse(0));
        content.update(0f);
        assertEquals(1, form.submits);
    }
}
