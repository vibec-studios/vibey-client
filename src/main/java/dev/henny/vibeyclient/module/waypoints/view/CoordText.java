package dev.henny.vibeyclient.module.waypoints.view;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.waypoints.core.Dimensions;
import dev.henny.vibeyclient.module.waypoints.core.NetherCoords;
import dev.henny.vibeyclient.module.waypoints.core.Pos;

public final class CoordText {
    private CoordText() {}

    public static String xyz(Pos p) {
        return p.x() + " " + p.y() + " " + p.z();
    }

    public static String xz(Pos p) {
        return p.x() + " " + p.z();
    }

    public static String dimensionName(String dimension) {
        if (Dimensions.NETHER.equals(dimension)) return Lang.tr("waypoints.dim.nether");
        if (Dimensions.END.equals(dimension)) return Lang.tr("waypoints.dim.end");
        if (Dimensions.OVERWORLD.equals(dimension)) return Lang.tr("waypoints.dim.overworld");
        int colon = dimension.indexOf(':');
        return colon < 0 ? dimension : dimension.substring(colon + 1);
    }

    public static String counterpart(Pos p, String dimension) {
        String other = dimension == null ? null : NetherCoords.counterpart(dimension);
        if (other == null) return "";
        Pos q = NetherCoords.convert(p, dimension, other);
        return "⇄ " + dimensionName(other) + " " + xz(q);
    }
}
