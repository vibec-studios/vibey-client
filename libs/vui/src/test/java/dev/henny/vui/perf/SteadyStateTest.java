package dev.henny.vui.perf;

import dev.henny.vui.CountingCanvas;
import dev.henny.vui.TestUi;
import dev.henny.vui.bind.Model;
import dev.henny.vui.dom.Element;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SteadyStateTest {
    private static final String PAGE = """
            <template>
              <column class="page" gap="2">
                <row class="bar"><text class="title">{{ title }}</text><spacer/><button>Save</button></row>
                <scroll class="list" height="400">
                  <row v-for="r in rows" :key="r.id" class="tile" :class="{ on: r.on }">
                    <icon glyph="*"/><text>{{ r.name }}</text><spacer/><toggle v-model="r.on"/><slider :value="r.v" width="40"/>
                  </row>
                </scroll>
              </column>
            </template>
            <style>
            .page { padding: 4; background: $bg; }
            .tile { padding: 2; gap: 4; border: 1 $line; background: $surface; }
            .tile.on { border-color: $brand; }
            .tile:hover { background: $surface2; transition: background 150ms out; }
            </style>
            """;

    private static TestUi page() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 30; i++) rows.add(new java.util.HashMap<>(Map.of("id", i, "name", "Module " + i, "on", i % 3 == 0, "v", i / 30.0)));
        Model m = new Model().value("title", "Settings");
        m.list("rows", rows);
        return TestUi.of(PAGE, m, 427, 480);
    }

    private static int count(Element e) {
        int n = 1;
        for (int i = 0; i < e.childCount(); i++) n += count(e.child(i));
        return n;
    }

    private static long allocated() {
        return ((com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean()).getCurrentThreadAllocatedBytes();
    }

    @Test
    void frameAndRenderAreAllocationFreeAfterWarmup() {
        TestUi ui = page();
        assertTrue(count(ui.doc.root()) >= 200, "nodes: " + count(ui.doc.root()));
        CountingCanvas canvas = new CountingCanvas();
        for (int i = 0; i < 2000; i++) {
            ui.doc.frame(1 / 60f);
            ui.doc.render(canvas);
        }
        long shortRun = measure(ui, canvas, 1000);
        long longRun = measure(ui, canvas, 5000);
        System.out.println("steady state: " + shortRun + " bytes / 1000 frames, " + longRun + " bytes / 5000 frames, "
                + canvas.total() / 8000 + " draw calls per frame");
        assertTrue(longRun - shortRun < 1024, "per-frame allocation: " + shortRun + " vs " + longRun);
        assertTrue(longRun < 4096, "allocated " + longRun + " bytes in 5000 steady frames");
        assertTrue(canvas.depth() == 0, "unbalanced push/pop");
    }

    private static long measure(TestUi ui, CountingCanvas canvas, int frames) {
        long before = allocated();
        for (int i = 0; i < frames; i++) {
            ui.doc.frame(1 / 60f);
            ui.doc.render(canvas);
        }
        return allocated() - before;
    }

    @Test
    void relayoutIsAllocationFreeAndFast() {
        TestUi ui = page();
        for (int i = 0; i < 500; i++) {
            ui.doc.invalidateLayout();
            ui.doc.layoutIfNeeded();
        }
        long before = allocated();
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            ui.doc.invalidateLayout();
            ui.doc.layoutIfNeeded();
        }
        long micros = (System.nanoTime() - start) / 1000 / 1000;
        long bytes = allocated() - before;
        System.out.println("relayout: " + micros + " us per pass, " + bytes / 1000 + " bytes per pass");
        assertTrue(micros < 2000, "relayout took " + micros + " us");
        assertTrue(bytes < 64 * 1000, "relayout allocated " + bytes + " bytes in 1000 passes");
    }

    @Test
    void frameTimingSanity() {
        TestUi ui = page();
        CountingCanvas canvas = new CountingCanvas();
        for (int i = 0; i < 500; i++) {
            ui.doc.frame(1 / 60f);
            ui.doc.render(canvas);
        }
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            ui.doc.frame(1 / 60f);
            ui.doc.render(canvas);
        }
        long micros = (System.nanoTime() - start) / 1000 / 1000;
        System.out.println("frame+render: " + micros + " us");
        assertTrue(micros < 1000, "frame+render took " + micros + " us");
    }

    @Test
    void hoverChangeRestylesOnlyTouchedSubtree() {
        TestUi ui = page();
        ui.doc.frame(0);
        int before = ui.doc.styles().restyles();
        Element tile = ui.first(".tile");
        ui.doc.pointerMove(tile.box().x + 1, tile.box().y + 1);
        ui.doc.frame(0);
        int restyled = ui.doc.styles().restyles() - before;
        assertTrue(restyled < 40, "restyled " + restyled + " elements for one hover");
    }
}
