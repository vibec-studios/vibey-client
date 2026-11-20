package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.servers.ServersModule;
import dev.henny.vibeyclient.module.servers.core.HoverDelay;
import dev.henny.vibeyclient.module.servers.core.ServerFilter;
import dev.henny.vibeyclient.module.servers.mixin.JoinMultiplayerAccess;
import dev.henny.vibeyclient.module.servers.mixin.ServerSelectionListInvoker;
import dev.henny.vibeyclient.module.servers.ping.LivePinger;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.host.DocumentView;
import dev.henny.vui.mc.input.KeyCodes;
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
    private final HoverDelay<ServerData> hover = new HoverDelay<>(HOVER_MS);
    private ServerData shownFor;
    private String lastQuery = "";
    private boolean lastFavOnly;
    private long fills;

    ServerSession(ServersModule m, JoinMultiplayerScreen screen) {
        this.m = m;
        this.screen = screen;
        this.barView = new DocumentView(VuiMc.vui().open(BAR, bar));
        this.previewView = new DocumentView(VuiMc.vui().open(PREVIEW, preview));
        this.pinger = new LivePinger(screen);
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

    ServerSelectionList list() {
        return ((JoinMultiplayerAccess) screen).vibey$list();
    }

    public void refreshList() {
        ServerSelectionList list = list();
        if (list != null) ((ServerSelectionListInvoker) list).vibey$refreshEntries();
    }

    void tick() {
        ServerSelectionList list = list();
        if (list != null) pinger.tick(m, list);
    }

    void render(Object graphics, int mouseX, int mouseY) {
        ServerSelectionList list = list();
        ServerSelectionList.Entry hovered = ServerHover.take();
        if (list == null || !m.isEnabled()) return;
        syncFilter(list);
        int rowLeft = list.getRowLeft(), rowRight = rowLeft + list.getRowWidth();
        bar.place(rowLeft, screen.width - rowRight, 16);
        bar.show(m.searchActive(), m.favouritesActive(), m.favs().size(), countOnline(list));
        if (m.searchActive() || m.favouritesActive()) {
            barView.move(mouseX, mouseY);
            barView.draw(graphics, screen.width, screen.height, false);
        }
        drawPreview(graphics, hovered);
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
        if (!barView.hits(x, y)) {
            barView.document().events().focus().blur();
            return true;
        }
        barView.down(x, y, button);
        return false;
    }

    boolean release(double x, double y, int button) {
        return !(barView.up(x, y, button) && barView.hits(x, y));
    }

    boolean key(int key, int scancode, boolean ctrl) {
        if (focused()) {
            if (key == Keys.ESCAPE) barView.document().events().focus().blur();
            else barView.keyDown(KeyCodes.fromNative(key), scancode);
            return false;
        }
        if (ctrl && key == Keys.F && m.searchActive()) {
            var field = barView.document().find("q");
            if (field != null) barView.document().events().focus().focus(field, true);
            return false;
        }
        return true;
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
        VuiMc.vui().close(barView.document());
        VuiMc.vui().close(previewView.document());
    }
}
