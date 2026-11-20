package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.edit.EditSession;
import dev.henny.vibeyclient.ui.hud.edit.PointerTool;
import dev.henny.vibeyclient.ui.hud.render.HudRenderer;
import dev.henny.vibeyclient.ui.vui.VibeyVui;
import dev.henny.vui.dom.Document;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.host.DocumentView;
import dev.henny.vui.mc.host.InputScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;

public final class HudEditorScreen extends InputScreen implements EditorHost {
    public static boolean drawPointer;

    private final Screen parent;
    private final EditSession session = new EditSession();
    private final PointerTool tool = new PointerTool(session);
    private final EditorController controller = new EditorController(session, this);
    private final EditorSync sync = new EditorSync(controller, tool);
    private final Decorations deco = new Decorations();
    private final EditorInput input = new EditorInput(this, controller, tool);
    private DocumentView view;
    private boolean closed;
    private double mouseX;
    private double mouseY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal(Lang.tr("ui.hud.title")));
        this.parent = parent;
        session.onToast(this::toast);
        tool.onDrag(() -> HudLayout.setFlag("intro", true));
    }

    @Override
    protected void init() {
        HudLayout.editing(true);
        session.refresh(width, height);
        if (view != null) return;
        Document doc = VibeyVui.get().open("vibeyclient:hud/editor", controller);
        doc.onEscape(input::escape);
        view = new DocumentView(doc);
    }

    @Override
    public void removed() {
        HudLayout.editing(false);
        if (!closed) HudLayout.save();
        closed = true;
        if (view != null) VuiMc.vui().close(view.document());
        view = null;
    }

    @Override
    public void onClose() {
        close(true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected boolean vanillaBackground() {
        return Minecraft.getInstance().level == null;
    }

    @Override
    public void close(boolean keep) {
        if (!keep) HudLayout.restore(session.opened);
        HudLayout.save();
        closed = true;
        HudLayout.editing(false);
        ClientCompat.openScreen(parent);
    }

    @Override
    public void refresh() {
        session.refresh(width, height);
    }

    @Override
    public void toast(String text) {
        VuiMc.toasts().push(text, "", null, "ok", 1.5f);
    }

    @Override
    public float pointerX() {
        return (float) mouseX;
    }

    @Override
    public float pointerY() {
        return (float) mouseY;
    }

    DocumentView view() {
        return view;
    }

    @Override
    protected void draw(Object graphics, int mx, int my, float delta) {
        if (view == null) return;
        session.refresh(width, height);
        tool.hover(mx, my, input.overChrome(mx, my));
        deco.under(graphics, session, tool, Minecraft.getInstance().level != null);
        HudRenderer.get().renderEditor(Gfx.of(graphics));
        deco.over(graphics, session, tool);
        sync.frame(view.document());
        deco.layer(graphics);
        view.draw(graphics, width, height, true);
        if (drawPointer) deco.pointer(graphics, mx, my);
    }

    @Override
    protected boolean pointerMove(double x, double y) {
        mouseX = x;
        mouseY = y;
        return input.move(x, y);
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        return input.down(x, y, button);
    }

    @Override
    protected boolean pointerUp(double x, double y, int button) {
        return input.up(x, y, button);
    }

    @Override
    protected boolean wheel(double x, double y, double dx, double dy) {
        return input.wheel(x, y, dx, dy);
    }

    @Override
    protected boolean keyDown(int key, int scancode) {
        return input.keyDown(key, scancode);
    }

    @Override
    protected boolean keyUp(int key, int scancode) {
        return view != null && view.keyUp(key, scancode);
    }

    @Override
    protected boolean typed(int codepoint) {
        return view != null && view.typed(codepoint);
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (paths != null && !paths.isEmpty()) controller.share.importFile(paths.get(0));
    }

    public EditSession session() {
        return session;
    }

    public EditorController controller() {
        return controller;
    }

    public PointerTool tool() {
        return tool;
    }

    public void select(String id) {
        session.selection.only(HudLayout.get(id));
    }

    public void addSelection(String id) {
        session.selection.add(HudLayout.get(id));
    }

    public Document document() {
        return view == null ? null : view.document();
    }
}
