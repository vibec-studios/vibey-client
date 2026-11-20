package dev.henny.vibeyclient.module.media.video.web;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.media.video.MediaSource;
import dev.henny.vibeyclient.media.player.MediaPlayer;
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
        ensureHandlers(api);
        String url = WebPlayer.initialUrl(source, startMs, playing);
        OUR_URLS.add(url);
        // MCEF creates the native browser asynchronously: load the page directly, no calls until it painted.
        MCEFBrowser browser = api.createBrowser(url, false);
        browser.resize(width, height);
        WebPlayer p = new WebPlayer(browser, source, url, width, height, startMs, playing);
        PLAYERS.put(browser.getCefBrowser(), p);
        return p;
    }

    static void forget(CefBrowser b, String url) {
        PLAYERS.remove(b);
        OUR_URLS.remove(url);
    }

    /** Start URLs of our browsers (the request handler sees them before we know the CefBrowser object). */
    private static final java.util.Set<String> OUR_URLS = ConcurrentHashMap.newKeySet();
    private static volatile boolean handlers;

    private static org.cef.CefClient client(MCEFApi api) {
        try {
            java.lang.reflect.Field f = api.getClass().getDeclaredField("client");
            f.setAccessible(true);
            return (org.cef.CefClient) f.get(api);
        } catch (ReflectiveOperationException | RuntimeException e) {
            MCEFBrowser tmp = api.createBrowser("about:blank", false);
            org.cef.CefClient c = tmp.getCefBrowser().getClient();
            tmp.close();
            return c;
        }
    }

    /**
     * Once, before our first browser: (1) the YouTube API-client identity – a Referer {@code https://<app id>} on our
     * browsers' YouTube navigations only (other mods' MCEF browsers are left alone); (2) a message router so pages
     * can report "state:posMs:durationMs".
     */
    private static synchronized void ensureHandlers(MCEFApi api) {
        if (handlers) return;
        handlers = true;
        org.cef.CefClient c = client(api);
        c.addRequestHandler(new org.cef.handler.CefRequestHandlerAdapter() {
            @Override
            public org.cef.handler.CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser, CefFrame frame,
                    org.cef.network.CefRequest request, boolean isNavigation, boolean isDownload, String initiator, org.cef.misc.BoolRef disable) {
                if (!isNavigation) return null;
                String url = request.getURL();
                boolean ours = PLAYERS.containsKey(browser) || url != null && OUR_URLS.contains(url);
                if (!ours || url == null || !isYouTube(url)) return null;
                return new org.cef.handler.CefResourceRequestHandlerAdapter() {
                    @Override
                    public boolean onBeforeResourceLoad(CefBrowser b, CefFrame f, org.cef.network.CefRequest r) {
                        r.setReferrer(WebPlayer.REFERER, org.cef.network.CefRequest.ReferrerPolicy.REFERRER_POLICY_DEFAULT);
                        return false;
                    }
                };
            }
        });
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
        c.addMessageRouter(router);
    }

    private static boolean isYouTube(String url) {
        try {
            String host = java.net.URI.create(url).getHost();
            return host != null && (host.equals("youtube.com") || host.endsWith(".youtube.com") || host.endsWith("youtube-nocookie.com"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
