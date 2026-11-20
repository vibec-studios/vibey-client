package dev.henny.vibeyclient.module.servers.friends;

import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.FriendsActivity;
import dev.henny.vibeyclient.module.servers.core.FriendSections;
import dev.henny.vibeyclient.module.servers.core.HoverDelay;
import dev.henny.vibeyclient.module.servers.ping.FriendPings;
import dev.henny.vibeyclient.ui.vui.Menus;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.host.DocumentView;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class FriendsTab implements FriendsController.Actions {
    public static final String DOC = "vibeyclient:servers/friends";
    static final long HOVER_MS = 400, REFRESH_MS = 1000;

    public interface DropZone {
        boolean over(double x, double y);

        void hot(boolean on);
    }

    private final JoinMultiplayerScreen screen;
    private final Supplier<ServerSelectionList> list;
    private final DropZone zone;
    private final FriendsController c = new FriendsController(this);
    private final DocumentView view;
    private final FriendPings pings;
    private final HoverDelay<String> hoverDelay = new HoverDelay<>(HOVER_MS);
    private final FriendCard card = new FriendCard(c);
    private String hovered;
    private FriendItem grabbed;
    private int seenRev = Integer.MIN_VALUE, seenPings = -1;
    private String seenState = "";
    private long rebuiltAt;

    public FriendsTab(JoinMultiplayerScreen screen, Supplier<ServerSelectionList> list, DropZone zone) {
        this.screen = screen;
        this.list = list;
        this.zone = zone;
        this.view = new DocumentView(VuiMc.vui().open(DOC, c));
        this.pings = new FriendPings(screen);
    }

    public FriendsController controller() {
        return c;
    }

    public DocumentView view() {
        return view;
    }

    public static FriendsActivity source() {
        return Services.get(FriendsActivity.class).orElse(null);
    }

    public int playing() {
        FriendsActivity src = source();
        return src == null || !FriendsActivity.READY.equals(src.state()) ? 0 : src.servers().size() + src.worlds().size();
    }

    public void tick() {
        FriendsActivity src = source();
        String st = src == null ? "off" : src.state();
        c.state(st);
        long now = System.currentTimeMillis();
        if (src == null) {
            if (!c.rows.snapshot().isEmpty()) c.rows.clear();
            return;
        }
        boolean due = src.revision() != seenRev || pings.version() != seenPings || !st.equals(seenState) || now - rebuiltAt > REFRESH_MS;
        if (due) rebuild(src, st, now);
    }

    private void rebuild(FriendsActivity src, String st, long now) {
        seenRev = src.revision();
        seenState = st;
        rebuiltAt = now;
        List<FriendsActivity.OnServer> servers = src.servers();
        List<String> addresses = new ArrayList<>();
        for (FriendsActivity.OnServer s : servers) if (s.shared()) addresses.add(s.address());
        pings.want(addresses);
        seenPings = pings.version();
        List<FriendItem> next = FriendItems.of(FriendSections.build(servers, src.worlds(), src.invites(), now), pings, now);
        if (!next.equals(c.rows.snapshot())) c.rows.replaceAll(next);
    }

    public void render(Object graphics, int mouseX, int mouseY, float x, float y, float w, float h) {
        c.place(x, y, w, h);
        card.update(find(hoverDelay.update(grabbed == null ? hovered : null, System.nanoTime())), mouseX, mouseY, screen.width, screen.height);
        view.move(mouseX, mouseY);
        view.draw(graphics, screen.width, screen.height, false);
    }

    private FriendItem find(String key) {
        if (key == null) return null;
        for (FriendItem i : c.rows.snapshot()) if (i.key().equals(key)) return i;
        return null;
    }

    public boolean hits(double x, double y) {
        return view.hits(x, y);
    }

    public void down(double x, double y, int button) {
        view.down(x, y, button);
    }

    public boolean up(double x, double y, int button) {
        return view.up(x, y, button);
    }

    public void scroll(double x, double y, double dx, double dy) {
        view.scroll(x, y, dx, dy);
    }

    public boolean dragging() {
        return grabbed != null;
    }

    public void cancelDrag() {
        grabbed = null;
        c.dragging.set(false);
        zone.hot(false);
    }

    public void close() {
        VuiMc.vui().close(view.document());
    }

    @Override
    public void join(FriendItem r) {
        FriendsActivity src = source();
        if (src == null || !r.canJoin()) return;
        switch (r.kind()) {
            case "server" -> src.joinServer(r.ref());
            case "world" -> src.joinWorld(r.ref());
            case "invite" -> src.accept(r.ref());
            default -> {}
        }
    }

    @Override
    public void add(FriendItem r) {
        AddServer.add(screen, list.get(), r.where(), r.address());
    }

    @Override
    public void decline(FriendItem r) {
        FriendsActivity src = source();
        if (src != null) src.decline(r.ref());
    }

    @Override
    public void openFriends() {
        FriendsActivity src = source();
        if (src != null) src.openFriends();
        else Menus.openFriends(screen);
    }

    @Override
    public void signIn() {
        FriendsActivity src = source();
        if (src != null) src.signIn();
    }

    @Override
    public void hover(FriendItem r) {
        hovered = r == null || r.header() ? null : r.key();
    }

    @Override
    public void grab(FriendItem r, float x, float y) {
        if (r == null || !r.canAdd()) return;
        grabbed = r;
        c.dragLabel.set(r.where());
        dragTo(x, y);
        c.dragging.set(true);
    }

    @Override
    public void dragTo(float x, float y) {
        if (grabbed == null) return;
        c.dragX.set(x + 8);
        c.dragY.set(y + 4);
        zone.hot(zone.over(x, y));
    }

    @Override
    public void dropAt(float x, float y) {
        FriendItem g = grabbed;
        boolean over = zone.over(x, y);
        cancelDrag();
        if (g != null && over) add(g);
    }
}
