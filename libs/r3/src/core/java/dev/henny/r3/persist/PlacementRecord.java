package dev.henny.r3.persist;

import com.google.gson.JsonObject;

public final class PlacementRecord {
    public String id;
    public String kind;
    public String owner;
    public String dimension;
    public double x;
    public double y;
    public double z;
    public float qx;
    public float qy;
    public float qz;
    public float qw = 1f;
    public float width = 1f;
    public float height = 1f;
    public float scale = 1f;
    public boolean locked;
    public boolean fullBright = true;
    public long revision;
    public AnchorRecord anchor;
    public JsonObject data;
}
