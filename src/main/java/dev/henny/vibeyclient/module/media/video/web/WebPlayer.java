package dev.henny.vibeyclient.module.media.video.web;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.henny.vibeyclient.module.media.video.MediaSource;
import dev.henny.vibeyclient.media.player.ExternalTexture;
import dev.henny.vibeyclient.media.player.MediaPlayer;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.cef.network.CefRequest;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A page in an off-screen Chromium (MCEF Modern). YouTube links open the official embedded player
 * ({@code youtube.com/embed/<id>}, identified with the Referer {@code https://dev.henny.vibeyclient} as YouTube asks
 * WebView apps to do) and are controlled only through the player's own API (play, pause, seek, volume). Nothing is
 * downloaded, nothing is drawn on top of the player. Chromium plays the sound itself (not positional): the distance
 * only lowers the page volume.
 */
public final class WebPlayer implements MediaPlayer {
    /** API client identity for the YouTube embed (Required Minimum Functionality: Referer https://<app id>). */
    public static final String REFERER = "https://dev.henny.vibeyclient/";
    private static final AtomicInteger IDS = new AtomicInteger();

    private final MCEFBrowser browser;
    private final MediaSource source;
    private final boolean youtube;
    private final int w;
    private final int h;
    private final Identifier texId;
    private final ExternalTexture tex = new ExternalTexture();
    private boolean registered;
    private boolean playing;
    private long baseMs;
    private long baseNanos;
    private int lastVolume = -1;
    private long lastPoll;
    private volatile int pageState = -99;
    private volatile long pagePos = -1;
    private volatile long pageDur = -1;
    private volatile long pageAt;
    private boolean closed;

    private final String url;
    /** Commands wait until the page painted once (the native browser is created asynchronously). */
    private boolean live;
    private long pendingSeek = -1;

    WebPlayer(MCEFBrowser browser, MediaSource source, String url, int width, int height, long startMs, boolean playing) {
        this.browser = browser;
        this.source = source;
        this.url = url;
        this.youtube = source.kind() == MediaSource.Kind.YOUTUBE;
        this.w = width;
        this.h = height;
        this.texId = Identifier.fromNamespaceAndPath("vibeyclient", "video/web_" + IDS.incrementAndGet());
        this.playing = playing;
        this.baseMs = startMs;
        this.baseNanos = System.nanoTime();
    }

    /** The page to open: YouTube links become the official embed player. */
    static String initialUrl(MediaSource source, long startMs, boolean autoplay) {
        if (source.kind() != MediaSource.Kind.YOUTUBE) return source.uri();
        return "https://www.youtube.com/embed/" + source.youTubeId() + "?enablejsapi=1&playsinline=1&rel=0&autoplay="
                + (autoplay ? 1 : 0) + (startMs >= 1000 ? "&start=" + startMs / 1000 : "");
    }

    public MCEFBrowser browser() {
        return browser;
    }

    private void js(String code) {
        if (!closed && live) browser.getCefBrowser().executeJavaScript("(function(){try{" + code + "}catch(e){}})();", url, 0);
    }

    private static final String MEDIA = "document.querySelectorAll('video,audio').forEach(function(m){%s});";

    @Override
    public void play() {
        if (!playing) {
            baseMs = positionMs();
            baseNanos = System.nanoTime();
        }
        playing = true;
        js(youtube ? "var p=document.getElementById('movie_player');p&&p.playVideo&&p.playVideo();" : String.format(MEDIA, "m.play();"));
    }

    @Override
    public void pause() {
        if (playing) baseMs = positionMs();
        playing = false;
        js(youtube ? "var p=document.getElementById('movie_player');p&&p.pauseVideo&&p.pauseVideo();" : String.format(MEDIA, "m.pause();"));
    }

    @Override
    public void seek(long ms) {
        baseMs = Math.max(0, ms);
        baseNanos = System.nanoTime();
        pagePos = -1;
        if (!live) {
            pendingSeek = baseMs;
            return;
        }
        String sec = String.format(Locale.ROOT, "%.2f", baseMs / 1000.0);
        js(youtube ? "var p=document.getElementById('movie_player');p&&p.seekTo&&p.seekTo(" + sec + ",true);"
                : String.format(MEDIA, "m.currentTime=" + sec + ";"));
    }

    /** From the page (CEF IO thread): "state:posMs:durMs". */
    void report(String s) {
        String[] p = s.split(":");
        if (p.length != 3) return;
        try {
            pageState = Integer.parseInt(p[0]);
            pagePos = Long.parseLong(p[1]);
            long d = Long.parseLong(p[2]);
            pageDur = d > 0 ? d : -1;
            pageAt = System.nanoTime();
        } catch (NumberFormatException ignored) {
            // malformed
        }
    }

    private boolean pageFresh() {
        return pagePos >= 0 && System.nanoTime() - pageAt < 2_000_000_000L;
    }

    @Override
    public long positionMs() {
        if (pageFresh()) {
            long p = pagePos;
            if (pageState == 1) p += (System.nanoTime() - pageAt) / 1_000_000L;
            return p;
        }
        return playing ? baseMs + (System.nanoTime() - baseNanos) / 1_000_000L : baseMs;
    }

    @Override
    public long durationMs() {
        return pageDur;
    }

    @Override
    public boolean seekable() {
        return pageFresh() && pageDur > 0;
    }

    @Override
    public Status status() {
        if (!tex.ready()) return Status.LOADING;
        if (pageFresh()) {
            if (pageState == 0) return Status.ENDED;
            if (pageState == 3 || pageState == -1 && playing) return Status.LOADING;
        }
        return playing ? Status.PLAYING : Status.PAUSED;
    }

    @Override
    public String error() {
        return null;
    }

    @Override
    public Identifier texture() {
        return tex.ready() ? texId : null;
    }

    @Override
    public int width() {
        return w;
    }

    @Override
    public int height() {
        return h;
    }

    @Override
    public boolean interactive() {
        return true;
    }

    @Override
    public void frame(boolean visible) {
        GpuTextureView v = browser.getTextureView();
        if (v != null) tex.set(browser.getTexture(), v);
        if (!registered && tex.ready()) {
            Minecraft.getInstance().getTextureManager().register(texId, tex);
            registered = true;
        }
        if (!live && tex.ready()) {
            live = true;
            lastVolume = -1;
            if (pendingSeek >= 0) {
                seek(pendingSeek);
                pendingSeek = -1;
            }
            if (playing) play();
            else pause();
        }
        long now = System.currentTimeMillis();
        if (now - lastPoll > 500) {
            lastPoll = now;
            // Until the page reports back it may not have had a player yet: send the volume again.
            if (!pageFresh()) lastVolume = -1;
            String report = "window." + McefBridge.QUERY + "&&window." + McefBridge.QUERY + "({request:'vv:'+s+':'+Math.round(t*1000)+':'+Math.round((d||0)*1000),onSuccess:function(){},onFailure:function(){}});";
            js(youtube
                    ? "var p=document.getElementById('movie_player');if(!p||!p.getCurrentTime)return;var s=p.getPlayerState(),t=p.getCurrentTime(),d=p.getDuration();" + report
                    : "var m=document.querySelector('video,audio');if(!m)return;var s=m.ended?0:(m.paused?2:1),t=m.currentTime,d=isFinite(m.duration)?m.duration:0;" + report);
        }
    }

    @Override
    public void audio(float gain, Vec3 pos) {
        int v = Math.round(Math.max(0f, Math.min(1f, gain)) * 100);
        if (Math.abs(v - lastVolume) < 2 && !(v == 0 && lastVolume != 0)) return;
        lastVolume = v;
        js(youtube ? "var p=document.getElementById('movie_player');if(p&&p.setVolume){if(" + v + ">0)p.unMute();else p.mute();p.setVolume(" + v + ");}"
                : String.format(MEDIA, "m.muted=" + (v == 0) + ";m.volume=" + v / 100.0 + ";"));
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        McefBridge.forget(browser.getCefBrowser(), url);
        if (registered) Minecraft.getInstance().getTextureManager().release(texId);
        browser.close();
    }
}
