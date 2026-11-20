package dev.henny.vibeyclient.module.media.video;

import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.anchor.FaceSnap;
import dev.henny.r3.math.Face;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.media.video.world.ScreenObjects;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public final class VideoDevPlace {
    private VideoDevPlace() {}

    static String place(Video v, String[] a) {
        if (a.length < 8) return "err place x y z normal width aspect source";
        MediaSource src = MediaSource.parse(String.join(" ", Arrays.copyOfRange(a, 7, a.length)));
        if (src == null) return "err bad source";
        Face normal = Face.byName(a[4].toLowerCase(Locale.ROOT));
        if (normal == null) return "err bad normal";
        MediaScreen s = place(v, src, Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]),
                normal, Float.parseFloat(a[5]), aspect(a[6]));
        return "ok " + s.id;
    }

    public static MediaScreen place(Video v, MediaSource src, double x, double y, double z, Face normal, float width, float aspect) {
        MediaScreen s = new MediaScreen(UUID.randomUUID());
        Minecraft mc = Minecraft.getInstance();
        s.owner = mc.player.getUUID();
        s.ownerName = ClientCompat.playerName();
        s.source = src;
        s.aspect = aspect;
        s.loop = true;
        s.setTimeline(true, 0);
        WorldObject o = ScreenObjects.create(s, v.dimension());
        Face up = normal.vertical() ? Face.NORTH : Face.UP;
        o.transform().position(x, y, z).size(width, width / aspect);
        FaceSnap.orientation(normal, up, o.transform().orientation());
        o.anchor(new BlockFaceAnchor(normal, up, (int) Math.floor(x - normal.stepX() * 0.5),
                (int) Math.floor(y - normal.stepY() * 0.5), (int) Math.floor(z - normal.stepZ() * 0.5)));
        ScreenObjects.fitLayout(o);
        R3Mc.add(o);
        v.actions.placed(s);
        return s;
    }

    private static float aspect(String s) {
        int c = s.indexOf(':');
        if (c > 0) return Float.parseFloat(s.substring(0, c)) / Float.parseFloat(s.substring(c + 1));
        return Float.parseFloat(s);
    }
}
