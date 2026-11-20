package dev.henny.r3.persist;

import dev.henny.r3.anchor.Anchor;
import dev.henny.r3.anchor.AnchorKind;
import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.anchor.CameraAnchor;
import dev.henny.r3.anchor.EntityAnchor;
import dev.henny.r3.anchor.Facing;
import dev.henny.r3.anchor.FreeAnchor;
import dev.henny.r3.anchor.LowersWhenLookingDown;
import dev.henny.r3.anchor.SoftFollowAnchor;
import dev.henny.r3.anchor.ViewFollowAnchor;
import dev.henny.r3.math.Face;
import org.joml.Vector3d;

import java.util.UUID;

public final class AnchorCodec {
    private AnchorCodec() {}

    public static AnchorRecord encode(Anchor anchor) {
        AnchorRecord r = new AnchorRecord();
        r.type = anchor.kind().name();
        switch (anchor) {
            case BlockFaceAnchor b -> {
                r.face = b.face().serializedName();
                r.up = b.up().serializedName();
                r.blockX = b.blockX();
                r.blockY = b.blockY();
                r.blockZ = b.blockZ();
            }
            case EntityAnchor e -> {
                r.entity = e.entity().toString();
                r.offsetX = e.offset().x();
                r.offsetY = e.offset().y();
                r.offsetZ = e.offset().z();
                r.aboveHead = e.aboveHead();
                r.facing = e.facing().name();
            }
            case CameraAnchor c -> {
                r.distance = c.distance();
                r.yawOffset = Float.isFinite(c.yawOffset()) ? c.yawOffset() : null;
                r.pitchOffset = c.pitchOffset();
                r.smoothing = c.smoothing();
            }
            case ViewFollowAnchor v -> r.distance = v.distance();
            case SoftFollowAnchor f -> {
                r.distance = f.distance();
                r.pitchOffset = f.elevation();
                r.yawOffset = Float.isFinite(f.startYaw()) ? f.startYaw() : null;
                r.smoothing = f.rate();
                r.recenter = f.recenterDegrees();
            }
            case LowersWhenLookingDown l -> {
                AnchorRecord inner = encode(l.inner());
                inner.lowers = true;
                return inner;
            }
            default -> {
            }
        }
        return r;
    }

    public static Anchor decode(AnchorRecord r) {
        if (r == null || r.type == null) return FreeAnchor.INSTANCE;
        AnchorKind kind = parseKind(r.type);
        if (kind == null) return FreeAnchor.INSTANCE;
        Anchor plain = plain(kind, r);
        return r.lowers && plain.followsSomething() ? new LowersWhenLookingDown(plain) : plain;
    }

    private static Anchor plain(AnchorKind kind, AnchorRecord r) {
        return switch (kind) {
            case BLOCK_FACE -> blockFace(r);
            case ENTITY -> entity(r);
            case CAMERA -> new CameraAnchor(finiteDistance(r.distance), r.yawOffset == null ? 0f : r.yawOffset, r.pitchOffset, r.smoothing);
            case VIEW_FOLLOW -> new ViewFollowAnchor(finiteDistance(r.distance));
            case SOFT_FOLLOW -> new SoftFollowAnchor(finiteDistance(r.distance), r.pitchOffset, r.recenter, r.smoothing, r.yawOffset == null ? Float.NaN : r.yawOffset);
            case FREE -> FreeAnchor.INSTANCE;
        };
    }

    private static Anchor blockFace(AnchorRecord r) {
        Face face = Face.byName(r.face);
        Face up = Face.byName(r.up);
        if (face == null || up == null || face.axis() == up.axis()) return FreeAnchor.INSTANCE;
        return new BlockFaceAnchor(face, up, r.blockX, r.blockY, r.blockZ);
    }

    private static Anchor entity(AnchorRecord r) {
        try {
            Facing facing = r.facing == null ? Facing.CAMERA_YAW : Facing.valueOf(r.facing);
            return new EntityAnchor(UUID.fromString(r.entity), new Vector3d(r.offsetX, r.offsetY, r.offsetZ), r.aboveHead, facing);
        } catch (IllegalArgumentException | NullPointerException e) {
            return FreeAnchor.INSTANCE;
        }
    }

    private static AnchorKind parseKind(String type) {
        try {
            return AnchorKind.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static double finiteDistance(double d) {
        return Double.isFinite(d) ? Math.max(0.5, Math.min(512, d)) : 2;
    }
}
