package dev.henny.vui;

import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;
import dev.henny.vui.dom.Element;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmokeTest {
    static final String CARD = """
            <template>
              <column id="card" class="card" gap="4">
                <text id="title" class="title">{{ title }}</text>
                <row v-for="m in modules" :key="m.id" class="tile" :class="{ on: m.enabled }" @click="open(m)">
                  <icon :src="m.icon"/> <text>{{ m.name }}</text> <toggle v-model="m.enabled"/>
                </row>
              </column>
            </template>
            <style>
            .card { padding: 8; background: $panel; border: 1 $line; }
            .tile:hover { background: $surface2; transition: background 150ms out; }
            </style>
            """;

    public static final class Module {
        public final String id;
        public final String name;
        public final String icon = "vibey:icons/gear";
        public final State<Boolean> enabled = new State<>(false);

        Module(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static final class Controller {
        public final State<String> title = new State<>("Modules");
        public final ListState<Module> modules = new ListState<>(List.of(new Module("a", "Alpha"), new Module("b", "Beta")));
        public Module opened;

        public void open(Module m) {
            opened = m;
        }
    }

    @Test
    void contractExampleBuildsLaysOutAndReacts() {
        Controller c = new Controller();
        TestUi ui = TestUi.of(CARD, c);
        assertTrue(ui.doc.diagnostics().isEmpty(), ui.errors());
        Element card = ui.find("card");
        assertEquals(3, card.childCount(), ui.dump());
        assertEquals("Modules", ui.find("title").text().chars().toString());
        c.title.set("Mods");
        ui.frame(0);
        assertEquals("Mods", ui.find("title").text().chars().toString());
        c.modules.add(new Module("c", "Gamma"));
        ui.frame(0);
        assertEquals(4, card.childCount(), ui.dump());
        Element tile = card.child(1);
        float x = tile.box().x + 3;
        float y = tile.box().y + tile.box().height / 2f;
        ui.doc.pointerMove(x, y);
        ui.doc.pointerDown(x, y, 0, 0);
        ui.doc.pointerUp(x, y, 0, 0);
        assertEquals("a", c.opened.id, ui.dump());
        Element toggle = ui.first("toggle");
        float tx = toggle.box().x + 2;
        float ty = toggle.box().y + 2;
        ui.doc.pointerMove(tx, ty);
        ui.doc.pointerDown(tx, ty, 0, 0);
        ui.doc.pointerUp(tx, ty, 0, 0);
        ui.frame(0);
        assertTrue(c.modules.get(0).enabled.peek(), ui.dump());
        assertTrue(tile.hasClass("on"), ui.dump());
        assertFalse(card.child(2).hasClass("on"));
        ui.render();
    }
}
