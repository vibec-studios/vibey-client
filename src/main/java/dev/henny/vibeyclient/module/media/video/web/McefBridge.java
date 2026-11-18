package dev.henny.vibeyclient.module.media.video.web;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.media.video.MediaSource;
import dev.henny.vibeyclient.module.media.video.player.MediaPlayer;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Everything that touches MCEF Modern classes. Only loaded when the mod is installed. */
final class McefBridge {
    /** JS function our pages call to report playback state (unique name, so other mods' routers don't clash). */
    static final String QUERY = "vibeyVideoQuery";
    private static volatile MCEFApi.Initialization init;
    private static volatile CefMessageRouter router;
    private static final Map<CefBrowser, WebPlayer> PLAYERS = new ConcurrentHashMap<>();

    private McefBridge() {}

    static boolean started() {
        return init != null;
    }

    static synchronized void start() {
        if (init == null) {
            init = MCEFApi.initialize();
            Vibey.LOG.info("Starting MCEF Modern for video screens");
        }
    }

    static Web.State state() {
        MCEFApi.Initialization i = init;
        if (i == null) return Web.State.STARTING;
        if (i.getFuture().isCompletedExceptionally()) return Web.State.FAILED;
        return i.getFuture().isDone() ? Web.State.READY : Web.State.STARTING;
    }

    static float percentage() {
        MCEFApi.Initialization i = init;
        return i == null ? -1f : i.getPercentage();
    }

    static String stage() {
        MCEFApi.Initialization i = init;
        return i == null ? "" : i.getStage().name();
    }

    static String failure() {
        MCEFApi.Initialization i = init;
        if (i == null || !i.getFuture().isCompletedExceptionally()) return null;
        try {
            i.getFuture().join();
        } catch (RuntimeException e) {
            Throwable t = e.getCause() != null ? e.getCause() : e;
            if (t.getCause() != null) t = t.getCause();
            return t.getMessage() != null ? t.getMessage() : t.toString();
        }
        return null;
    }

    static MediaPlayer create(MediaSource source, int width, int height, long startMs, boolean playing) {
        MCEFApi api = init.getFuture().getNow(null);
        if (api == null) throw new IllegalStateException("MCEF not ready");
        MCEFBrowser browser = api.createBrowser("about:blank", false);
        browser.resize(width, height);
        ensureRouter(browser.getCefBrowser());
        WebPlayer p = new WebPlayer(browser, source, width, height, startMs, playing);
        PLAYERS.put(browser.getCefBrowser(), p);
        return p;
    }

    static void forget(CefBrowser b) {
        PLAYERS.remove(b);
    }

    /** One message router on MCEF's client for all our browsers: pages report "state:posMs:durationMs". */
    private static synchronized void ensureRouter(CefBrowser b) {
        if (router != null) return;
        router = CefMessageRouter.create(new CefMessageRouter.CefMessageRouterConfig(QUERY, QUERY + "Cancel"));
        router.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                WebPlayer p = PLAYERS.get(browser);
                if (p == null || request == null || !request.startsWith("vv:")) return false;
                p.report(request.substring(3));
                callback.success("");
                return true;
            }
        }, true);
        b.getClient().addMessageRouter(router);
    }
}
