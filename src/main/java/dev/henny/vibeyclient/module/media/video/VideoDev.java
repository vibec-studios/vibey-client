package dev.henny.vibeyclient.module.media.video;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.dev.DevDriver;
import dev.henny.vibeyclient.module.media.video.player.Ffmpeg;
import dev.henny.vibeyclient.module.media.video.ui.ControlsScreen;
import dev.henny.vibeyclient.module.media.video.ui.SetupScreen;
import dev.henny.vibeyclient.module.media.video.ui.SourceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * DevDriver commands for testing (dev runs with {@code -Dvibeyclient.devDriver=<port>} only):
 * <pre>
 * video list | video place &lt;x&gt; &lt;y&gt; &lt;z&gt; &lt;normal&gt; &lt;width&gt; &lt;aspect&gt; &lt;source...&gt; | video placing &lt;source&gt;
 * video play|pause|controls|overlay|accept|remove &lt;index&gt; | video seek &lt;index&gt; &lt;ms&gt;
 * video new | video others ask|always|never | video setup ffmpeg|mod|natives | video hideffmpeg on|off | video edit
 * </pre>
 */
final class VideoDev {
    private VideoDev() {}

    static void register(Video v) {
        if (!DevDriver.active()) return;
        DevDriver.EXTRA.put("video", arg -> exec(v, arg));
    }

    private static MediaScreen at(Video v, String idx) {
        List<MediaScreen> all = new ArrayList<>(v.screens.values());
        int i = Integer.parseInt(idx.trim());
        return i >= 0 && i < all.size() ? all.get(i) : null;
    }

    private static String exec(Video v, String arg) {
        String[] a = arg.split(" ");
        String sub = a[0].toLowerCase(Locale.ROOT);
        Minecraft mc = Minecraft.getInstance();
        switch (sub) {
            case "list": {
                StringBuilder sb = new StringBuilder("ok");
                int i = 0;
                for (MediaScreen s : v.screens.values()) {
                    String st = s.player == null ? "none" : s.player.status().name();
                    sb.append(String.format(Locale.ROOT, " [%d %s %s remote=%s accepted=%s player=%s pos=%d/%d exp=%d tex=%s status=%s]", i++,
                            s.source == null ? "-" : s.source.kind(), s.source == null ? "-" : s.source.label(), s.remote, s.accepted, st,
                            s.player == null ? -1 : s.player.positionMs(), s.durationMs, s.expectedMs(),
                            s.player != null && s.player.texture() != null, v.statusLine(s)));
                }
                return sb.toString();
            }
            case "place": {
                if (a.length < 8) return "err place x y z normal width aspect source";
                MediaSource src = MediaSource.parse(String.join(" ", java.util.Arrays.copyOfRange(a, 7, a.length)));
                if (src == null) return "err bad source";
                float aspect = parseAspect(a[6]);
                MediaScreen s = new MediaScreen(UUID.randomUUID());
                s.owner = mc.player.getUUID();
                s.ownerName = ClientCompat.playerName();
                s.source = src;
                s.dimension = v.dimension();
                s.x = Double.parseDouble(a[1]);
                s.y = Double.parseDouble(a[2]);
                s.z = Double.parseDouble(a[3]);
                s.normal = Direction.byName(a[4].toLowerCase(Locale.ROOT));
                s.up = s.normal.getAxis().isVertical() ? Direction.NORTH : Direction.UP;
                s.width = Float.parseFloat(a[5]);
                s.height = s.width / aspect;
                s.loop = true;
                s.setTimeline(true, 0);
                v.placed(s, false);
                return "ok " + s.id;
            }
            case "placing": {
                MediaSource src = MediaSource.parse(arg.substring(sub.length()).trim());
                if (src == null) return "err bad source";
                v.create(src, 16f / 9f, true);
                return "ok";
            }
            case "new":
                ClientCompat.openScreen(new SourceScreen(null, null));
                return "ok";
            case "play":
            case "pause": {
                MediaScreen s = at(v, a[1]);
                if (s == null) return "err no screen";
                v.setPlaying(s, sub.equals("play"));
                return "ok";
            }
            case "seek": {
                MediaScreen s = at(v, a[1]);
                if (s == null) return "err no screen";
                v.seek(s, Long.parseLong(a[2]));
                return "ok";
            }
            case "controls": {
                MediaScreen s = at(v, a[1]);
                if (s == null) return "err no screen";
                ClientCompat.openScreen(new ControlsScreen(null, s));
                return "ok";
            }
            case "open": {
                MediaScreen s = at(v, a[1]);
                if (s == null) return "err no screen";
                v.openControls(s);
                return "ok";
            }
            case "overlay": {
                MediaScreen s = at(v, a[1]);
                if (s == null) return "err no screen";
                v.toggleOverlay(s);
                return "ok overlay=" + v.overlayId;
            }
            case "accept": {
                MediaScreen s = at(v, a[1]);
                if (s == null) return "err no screen";
                s.accepted = true;
                return "ok";
            }
            case "remove": {
                MediaScreen s = at(v, a[1]);
                if (s == null) return "err no screen";
                v.remove(s);
                return "ok";
            }
            case "others":
                v.cfg.others.set(VideoSettings.Others.valueOf(a[1].toUpperCase(Locale.ROOT)));
                return "ok";
            case "setup":
                ClientCompat.openScreen(new SetupScreen(null, SetupScreen.Need.valueOf(switch (a[1]) {
                    case "mod" -> "BROWSER_MOD";
                    case "natives" -> "BROWSER_NATIVES";
                    default -> "FFMPEG";
                })));
                return "ok";
            case "hideffmpeg":
                Ffmpeg.hiddenForTest = a[1].equals("on");
                Ffmpeg.refresh();
                return "ok available=" + Ffmpeg.available();
            case "edit":
                v.editOverlay();
                return "ok";
            default:
                return "err video " + sub;
        }
    }

    private static float parseAspect(String s) {
        int c = s.indexOf(':');
        if (c > 0) return Float.parseFloat(s.substring(0, c)) / Float.parseFloat(s.substring(c + 1));
        return Float.parseFloat(s);
    }
}
