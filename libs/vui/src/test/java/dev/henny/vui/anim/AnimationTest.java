package dev.henny.vui.anim;

import dev.henny.vui.TestUi;
import dev.henny.vui.bind.Model;
import dev.henny.vui.dom.Element;
import dev.henny.vui.style.Prop;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationTest {
    @AfterEach
    void resetGlobals() {
        Animations.setEnabled(true);
        Animations.setSpeed(1f);
    }

    @Test
    void vibeEasingMatchesCubicBezier() {
        Easing out = Easings.OUT;
        assertEquals(0f, out.apply(0f));
        assertEquals(1f, out.apply(1f));
        assertTrue(out.apply(0.5f) > 0.85f, "out is front-loaded: " + out.apply(0.5f));
        float prev = 0;
        for (int i = 1; i <= 100; i++) {
            float v = out.apply(i / 100f);
            assertTrue(v >= prev - 1e-4f, "monotonic");
            prev = v;
        }
        assertEquals(0.5f, Easings.LINEAR.apply(0.5f));
        assertEquals(Easings.parse("cubic-bezier(0.2, 0.8, 0.2, 1)").apply(0.3f), out.apply(0.3f), 1e-4);
    }

    @Test
    void tweenTimingAndDelay() {
        Tween t = new Tween(0).start(0, 100, 0.2f, 0.1f, Easings.LINEAR);
        t.step(0.1f);
        assertEquals(0f, t.value(), 0.01);
        t.step(0.1f);
        assertEquals(50f, t.value(), 0.5);
        t.step(0.11f);
        assertFalse(t.running());
        assertEquals(100f, t.value());
    }

    @Test
    void springSettlesOnTarget() {
        Spring s = Spring.standard(0);
        s.target(10);
        for (int i = 0; i < 120; i++) s.step(1 / 60f);
        assertTrue(s.atRest());
        assertEquals(10f, s.value());
    }

    @Test
    void cssTransitionAnimatesColorOverDuration() {
        Model m = new Model().value("on", false);
        TestUi ui = TestUi.of("""
                <template><box id="b" :class="{ on }" size="10"/></template>
                <style>#b { background: #000000; transition: background 100ms linear; } #b.on { background: #ffffff; }</style>
                """, m);
        Element b = ui.find("b");
        m.value("on", true);
        ui.frame(0);
        assertEquals(0xFF000000, b.style().color(Prop.BACKGROUND));
        ui.frame(0.05f);
        int mid = b.style().color(Prop.BACKGROUND);
        int red = (mid >> 16) & 0xFF;
        assertTrue(red > 100 && red < 160, Integer.toHexString(mid));
        ui.frame(0.06f);
        assertEquals(0xFFFFFFFF, b.style().color(Prop.BACKGROUND));
        assertEquals(0xFFFFFFFF, b.computed().color(Prop.BACKGROUND));
    }

    @Test
    void animationsOffSnapsImmediately() {
        Animations.setEnabled(false);
        Model m = new Model().value("on", false);
        TestUi ui = TestUi.of("<template><box id=\"b\" :class=\"{ on }\" size=\"10\"/></template><style>#b { opacity: 0.2; transition: opacity 1s; } #b.on { opacity: 1; }</style>", m);
        m.value("on", true);
        ui.frame(0);
        assertEquals(1f, ui.find("b").style().number(Prop.OPACITY));
    }

    @Test
    void transitionOnLayoutPropRelayouts() {
        Model m = new Model().value("wide", false);
        TestUi ui = TestUi.of("<template><column><box id=\"b\" :class=\"{ wide }\" height=\"5\"/></column></template><style>#b { width: 10; transition: width 100ms linear; } #b.wide { width: 50; }</style>", m);
        m.value("wide", true);
        ui.frame(0).frame(0.05f);
        assertEquals(30f, ui.find("b").box().width, 1f);
        ui.frame(0.06f);
        assertEquals(50f, ui.find("b").box().width, 0.01f);
    }

    @Test
    void keyframesLoopAndAlternate() {
        TestUi ui = TestUi.of("""
                <template><box id="b" size="10"/></template>
                <style>#b { animation: fade 1s infinite alternate; } @keyframes fade { from { opacity: 1 } to { opacity: 0 } }</style>
                """, null);
        Element b = ui.find("b");
        ui.frame(0.25f);
        assertEquals(0.75f, b.style().number(Prop.OPACITY), 0.02);
        ui.frame(1.0f);
        assertEquals(0.25f, b.style().number(Prop.OPACITY), 0.02);
    }

    @Test
    void unknownKeyframesReported() {
        TestUi ui = TestUi.of("<template><box style=\"animation: nope 1s\"/></template>", null);
        assertTrue(ui.errors().contains("unknown @keyframes 'nope'"), ui.errors());
    }

    @Test
    void enterAndLeaveTransitionsForVIf() {
        Model m = new Model().value("show", false);
        TestUi ui = TestUi.of("""
                <template><column><box v-if="show" id="b" class="fade" size="10"/></column></template>
                <style>.fade { opacity: 1; transition: opacity 100ms linear; } .fade:enter, .fade:leave { opacity: 0; }</style>
                """, m);
        m.value("show", true);
        ui.frame(0);
        Element b = ui.find("b");
        assertEquals(0f, b.style().number(Prop.OPACITY), 0.01, "anim=" + (b.animator() == null ? "none" : b.animator().running()) + " computed=" + b.computed().number(Prop.OPACITY) + " states=" + dev.henny.vui.style.StateFlags.names(b.states()) + " enterRules=" + ui.doc.styles().hasEnterRules() + " entering=" + b.flags().entering + " mounted=" + b.flags().mounted + " styled=" + b.flags().styledOnce);
        ui.frame(0.05f);
        assertEquals(0.5f, b.style().number(Prop.OPACITY), 0.05);
        ui.frame(0.06f);
        assertEquals(1f, b.style().number(Prop.OPACITY), 0.01);
        m.value("show", false);
        ui.frame(0);
        assertTrue(b.flags().leaving);
        ui.frame(0.05f);
        assertNotEquals(null, ui.doc.find("b"));
        ui.frame(0.06f).frame(0.01f);
        assertNull(ui.doc.find("b"));
    }
}
