package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.UiScale;
import dev.henny.vibeyclient.ui.vui.VibeyVui;
import dev.henny.vui.dom.Element;
import dev.henny.vui.mc.host.VuiScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class MenuScreen extends VuiScreen implements MenuHost {
    public static final String DOC = "vibeyclient:menu/shell";

    private final MenuController controller;
    private final Screen back;
    private int ticks;

    private MenuScreen(Screen parent, MenuController controller) {
        super(DOC, controller);
        this.controller = controller;
        this.back = parent;
        parent(parent);
        background(!ClientCompat.inWorld());
        controller.attach(this);
    }

    public static MenuScreen open(Screen parent, String moduleId, String panel) {
        VibeyVui.init();
        MenuController c = new MenuController();
        c.preset(moduleId, panel == null || panel.isEmpty() ? null : panel);
        MenuScreen s = new MenuScreen(parent, c);
        ClientCompat.openScreen(s);
        return s;
    }

    public MenuController controller() {
        return controller;
    }

    @Override
    public Screen screen() {
        return this;
    }

    @Override
    public void closeNow() {
        super.onClose();
    }

    @Override
    public void onClose() {
        if (!controller.escape()) closeNow();
    }

    @Override
    protected void init() {
        UiScale.apply(this);
        super.init();
    }

    @Override
    public void removed() {
        super.removed();
        UiScale.restore();
    }

    @Override
    public boolean isPauseScreen() {
        return back != null && back.isPauseScreen();
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks % 5 != 0) return;
        controller.sync();
        document().refresh();
    }

    @Override
    protected boolean keyDown(int key, int scancode) {
        if (view().keyDown(key, scancode)) return true;
        if (!typing() && ClientCompat.matches(GlobalSettings.openMenu.mapping(), key)) {
            closeNow();
            return true;
        }
        return false;
    }

    @Override
    protected boolean typed(int codepoint) {
        if (view().typed(codepoint)) return true;
        if (codepoint <= 32 || Character.isISOControl(codepoint)) return false;
        List<Element> fields = document().query(".search-field");
        if (fields.isEmpty()) return false;
        controller.back();
        document().events().focus().focus(fields.get(0), true);
        return view().typed(codepoint);
    }

    private boolean typing() {
        Element f = document().events().focus().focused();
        return f != null && (f.tag().equals("textfield") || f.tag().equals("keybind"));
    }
}
