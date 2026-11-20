package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.servers.ServersModule;
import dev.henny.vibeyclient.module.servers.core.HoverDelay;
import dev.henny.vibeyclient.module.servers.core.ServerFilter;
import dev.henny.vibeyclient.module.servers.drag.ListDrag;
import dev.henny.vibeyclient.module.servers.drag.ServerDrop;
import dev.henny.vibeyclient.module.servers.friends.FriendsTab;
import dev.henny.vibeyclient.module.servers.mixin.JoinMultiplayerAccess;
import dev.henny.vibeyclient.module.servers.mixin.ServerSelectionListInvoker;
import dev.henny.vibeyclient.module.servers.ping.LivePinger;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.host.DocumentView;
import dev.henny.vui.mc.input.KeyCodes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;

public final class ServerSession {
    public static final String BAR = "vibeyclient:servers/bar", PREVIEW = "vibeyclient:servers/preview";
    static final long HOVER_MS = 350;

    private final ServersModule m;
    private final JoinMultiplayerScreen screen;
    private final BarController bar = new BarController();
    private final PreviewController preview = new PreviewController();
    private final DocumentView barView;
    private final DocumentView previewView;
    private final LivePinger pinger;
    private final FriendsTab friends;
    private final VanillaChrome chrome;
    private final HoverDelay<ServerData> hover = new HoverDelay<>(HOVER_MS);
    private ServerData shownFor;
    private String lastQuery = "";
    private boolean lastFavOnly;
    private long fills;
    private ListDrag drag;
    private ServerSelectionList dragList;

    ServerSession(ServersModule m, JoinMultiplayerScreen screen) {
        this.m = m;
        this.screen = screen;
        this.barView = new DocumentView(VuiMc.vui().open(BAR, bar));
        this.previewView = new DocumentView(VuiMc.vui().open(PREVIEW, preview));
        this.pinger = new LivePinger(screen);
        this.chrome = new VanillaChrome(screen);
        this.friends = new FriendsTab(screen, this::list, new TabDropZone(barView, bar));
        ServerListView.filter(ServerFilter.ALL);
        refreshList();
    }

    public BarController bar() {
        return bar;
    }

    public PreviewController preview() {
        return preview;
    }

    public LivePinger pinger() {
        return pinger;
    }

    public FriendsTab friends() {
        return friends;
    }

    public DocumentView barView() {
        return barView;
    }

    public ServerSelectionList list() {
        return ((JoinMultiplayerAccess) screen).vibey$list();
    }

    public ListDrag drag() {
        ServerSelectionList list = list();
        if (list == null) return null;
        if (drag == null || dragList != list) {
            if (drag != null) drag.cancel();
            dragList = list;
            drag = new ListDrag(list, new ServerDrop(m, screen, list));
        }
        return drag;
    }

    public void refreshList() {
        ServerSelectionList list = list();
        if (list != null) ((ServerSelectionListInvoker) list).vibey$refreshEntries();
    }

    private boolean friendsOn() {
        return m.friendsActive() && bar.friends();
    }

    void tick() {
        ServerSelectionList list = list();
        if (list != null) pinger.tick(m, list);
        if (m.friendsActive()) friends.tick();
    }

    void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        ServerSelectionList list = list();
        ServerSelectionList.Entry hovered = ServerHover.take();
        if (list == null || !m.isEnabled()) return;
        syncFilter(list);
        boolean tabs = m.friendsActive();
        boolean friendsShown = friendsOn();
        chrome.apply(list, tabs, friendsShown);
        int rowLeft = list.getRowLeft(), rowRight = rowLeft + list.getRowWidth();
        bar.place(rowLeft, screen.width - rowRight, 16);
        bar.tabs(tabs, tabs ? friends.playing() : 0);
        bar.show(m.searchActive(), m.favouritesActive(), m.favs().size(), countOnline(list));
        if (friendsShown) {
            friends.render(graphics, mouseX, mouseY, rowLeft, list.getY(), list.getRowWidth(), list.getHeight());
        } else {
            ListDrag d = drag();
            if (d != null) {
                d.frame(mouseX, mouseY);
                d.drawFloating(graphics, mouseY, delta);
            }
        }
        if (tabs || m.searchActive() || m.favouritesActive()) {
            barView.move(mouseX, mouseY);
            barView.draw(graphics, screen.width, screen.height, false);
        }
        boolean dragging = drag != null && drag.dragging();
        drawPreview(graphics, friendsShown || dragging ? null : hovered);
    }

    private void drawPreview(Object graphics, ServerSelectionList.Entry hovered) {
        ServerData d = m.previewActive() && hovered instanceof ServerSelectionList.OnlineServerEntry o ? o.getServerData() : null;
        ServerData show = hover.update(d, System.nanoTime());
        if (show == null) {
            preview.hide();
            shownFor = null;
            return;
        }
        if (show != shownFor || frameDue()) preview.fill(PreviewData.of(show, m.favourite(show.ip)));
        shownFor = show;
        int cardH = 30 + preview.lines.snapshot().size() * 10 + (preview.sample.peek().isEmpty() ? 0 : 10);
        float y = hovered.getContentY() + hovered.getContentHeight() + 3;
        if (y + cardH > screen.height - 4) y = hovered.getContentY() - cardH - 3;
        preview.place(hovered.getContentX() + 35, Math.max(2, y));
        previewView.draw(graphics, screen.width, screen.height, false);
    }

    private boolean frameDue() {
        return ++fills % 20 == 0;
    }

    private void syncFilter(ServerSelectionList list) {
        String q = m.searchActive() ? bar.query.peek() : "";
        boolean favOnly = m.favouritesActive() && bar.favOnly.peek();
        if (q.equals(lastQuery) && favOnly == lastFavOnly) return;
        lastQuery = q;
        lastFavOnly = favOnly;
        ServerListView.filter(ServerFilter.of(q, favOnly));
        ((ServerSelectionListInvoker) list).vibey$refreshEntries();
        list.setScrollAmount(0);
    }

    private static int countOnline(ServerSelectionList list) {
        int n = 0;
        for (ServerSelectionList.Entry e : list.children()) if (e instanceof ServerSelectionList.OnlineServerEntry) n++;
        return n;
    }

    boolean click(double x, double y, int button) {
        if (barView.hits(x, y)) {
            barView.down(x, y, button);
            return false;
        }
        barView.document().events().focus().blur();
        if (friendsOn()) {
            if (friends.hits(x, y)) friends.down(x, y, button);
            return !insideList(x, y) && !friends.hits(x, y);
        }
        ListDrag d = drag();
        if (d != null && m.dragActive()) d.press(x, y, button);
        return true;
    }

    boolean release(double x, double y, int button) {
        if (friendsOn()) {
            boolean used = friends.up(x, y, button);
            barView.up(x, y, button);
            return !used && !insideList(x, y);
        }
        ListDrag d = drag();
        if (d != null && d.release()) return false;
        return !(barView.up(x, y, button) && barView.hits(x, y));
    }

    boolean scroll(double x, double y, double dx, double dy) {
        if (!friendsOn()) return true;
        friends.scroll(x, y, dx, dy);
        return !insideList(x, y);
    }

    private boolean insideList(double x, double y) {
        ServerSelectionList list = list();
        return list != null && x >= list.getX() && x < list.getRight() && y >= list.getY() && y < list.getBottom();
    }

    boolean key(int key, int scancode, boolean ctrl) {
        if (key == Keys.ESCAPE && drag != null && drag.dragging()) {
            drag.cancel();
            return false;
        }
        if (key == Keys.ESCAPE && friends.dragging()) {
            friends.cancelDrag();
            return false;
        }
        if (focused()) {
            if (key == Keys.ESCAPE) barView.document().events().focus().blur();
            else barView.keyDown(KeyCodes.fromNative(key), scancode);
            return false;
        }
        if (ctrl && key == Keys.F && m.searchActive()) {
            bar.showServers();
            var field = barView.document().find("q");
            if (field != null) barView.document().events().focus().focus(field, true);
            return false;
        }
        return !friendsOn() || key == Keys.ESCAPE;
    }

    boolean typed(int codepoint) {
        return focused() && barView.typed(codepoint);
    }

    private boolean focused() {
        return barView.document().events().focus().focused() != null;
    }

    void close() {
        ServerHover.clear();
        ServerListView.filter(ServerFilter.ALL);
        if (drag != null) drag.cancel();
        chrome.restore(list());
        friends.close();
        VuiMc.vui().close(barView.document());
        VuiMc.vui().close(previewView.document());
    }
}
