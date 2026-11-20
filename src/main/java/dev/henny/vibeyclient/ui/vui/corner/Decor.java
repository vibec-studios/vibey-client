package dev.henny.vibeyclient.ui.vui.corner;

import dev.henny.vibeyclient.ui.vui.VibeyVui;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.mc.host.DocumentView;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class Decor {
    final Screen screen;
    private final CornerController controller;
    private final Document doc;
    private final DocumentView view;
    private final List<ProxyButton> proxies = new ArrayList<>();

    Decor(Screen screen, String docId, CornerController controller, Consumer<AbstractWidget> add) {
        this.screen = screen;
        this.controller = controller;
        this.doc = VibeyVui.vui().open(docId, controller);
        this.view = new DocumentView(doc);
        doc.resize(screen.width, screen.height);
        doc.frame(0);
        doc.layoutIfNeeded();
        for (CornerEntry e : controller.all()) {
            ProxyButton p = new ProxyButton(e.getId(), e.getLabel(), () -> e.entry().action().accept(screen));
            proxies.add(p);
            add.accept(p);
        }
        place();
    }

    void render(Object graphics, int mouseX, int mouseY) {
        String focused = "";
        for (ProxyButton p : proxies) if (p.isFocused()) focused = p.id;
        controller.focus.set(focused);
        view.move(mouseX, mouseY);
        view.draw(graphics, screen.width, screen.height, false);
        place();
    }

    List<ProxyButton> proxies() {
        return proxies;
    }

    void close() {
        VibeyVui.vui().close(doc);
    }

    private void place() {
        for (ProxyButton p : proxies) {
            Element e = doc.find("e-" + p.id);
            if (e == null) continue;
            LayoutBox b = e.box();
            p.place(b.x, b.y, b.width, b.height);
        }
    }
}
