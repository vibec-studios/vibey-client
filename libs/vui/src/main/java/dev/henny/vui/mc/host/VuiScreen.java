package dev.henny.vui.mc.host;

import dev.henny.vui.dom.Document;
import dev.henny.vui.mc.VuiRuntime;
import dev.henny.vui.mc.compat.GuiScale;
import dev.henny.vui.mc.compat.Mc;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VuiScreen extends InputScreen {
    private final String documentId;
    private final Object controller;
    private Screen parent;
    private int guiScale;
    private boolean background = true;
    private boolean pause = true;
    private DocumentView view;

    public VuiScreen(String documentId, Object controller) {
        super(Component.literal(documentId));
        this.documentId = documentId;
        this.controller = controller;
    }

    public VuiScreen parent(Screen screen) {
        parent = screen;
        return this;
    }

    public VuiScreen guiScale(int scale) {
        guiScale = scale;
        return this;
    }

    public VuiScreen background(boolean on) {
        background = on;
        return this;
    }

    public VuiScreen pause(boolean on) {
        pause = on;
        return this;
    }

    public VuiScreen open() {
        Mc.setScreen(this);
        return this;
    }

    public String documentId() {
        return documentId;
    }

    public Object controller() {
        return controller;
    }

    public Document document() {
        return view().document();
    }

    public DocumentView view() {
        if (view == null) {
            Document doc = VuiRuntime.get().vui().open(documentId, controller);
            doc.onEscape(this::onClose);
            doc.resize(Mc.guiWidth(), Mc.guiHeight());
            view = new DocumentView(doc);
        }
        return view;
    }

    @Override
    protected void init() {
        GuiScale.apply(guiScale);
        width = Mc.guiWidth();
        height = Mc.guiHeight();
        view();
    }

    @Override
    public void removed() {
        GuiScale.restore();
        if (view == null) return;
        VuiRuntime.get().vui().close(view.document());
        view = null;
    }

    @Override
    public void onClose() {
        Mc.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return pause;
    }

    @Override
    protected boolean vanillaBackground() {
        return background;
    }

    @Override
    protected void draw(Object graphics, int mouseX, int mouseY, float delta) {
        view().draw(graphics, width, height, true);
    }

    @Override
    protected boolean pointerMove(double x, double y) {
        return view().move(x, y);
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        return view().down(x, y, button);
    }

    @Override
    protected boolean pointerUp(double x, double y, int button) {
        return view().up(x, y, button);
    }

    @Override
    protected boolean wheel(double x, double y, double dx, double dy) {
        return view().scroll(x, y, dx, dy);
    }

    @Override
    protected boolean keyDown(int key, int scancode) {
        return view().keyDown(key, scancode);
    }

    @Override
    protected boolean keyUp(int key, int scancode) {
        return view().keyUp(key, scancode);
    }

    @Override
    protected boolean typed(int codepoint) {
        return view().typed(codepoint);
    }
}
