package dev.henny.r3.anchor;

import dev.henny.r3.math.Face;
import dev.henny.r3.object.Transform;

public final class BlockFaceAnchor implements Anchor {
    private final Face face;
    private final Face up;
    private final int blockX;
    private final int blockY;
    private final int blockZ;

    public BlockFaceAnchor(Face face, Face up, int blockX, int blockY, int blockZ) {
        if (face.axis() == up.axis()) throw new IllegalArgumentException("up must be perpendicular to the face");
        this.face = face;
        this.up = up;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }

    public static BlockFaceAnchor of(BlockHit hit, Face up) {
        return new BlockFaceAnchor(hit.face(), up, hit.blockX(), hit.blockY(), hit.blockZ());
    }

    public Face face() {
        return face;
    }

    public Face up() {
        return up;
    }

    public int blockX() {
        return blockX;
    }

    public int blockY() {
        return blockY;
    }

    public int blockZ() {
        return blockZ;
    }

    public boolean onFloorOrCeiling() {
        return face.vertical();
    }

    @Override
    public AnchorKind kind() {
        return AnchorKind.BLOCK_FACE;
    }

    @Override
    public boolean resolve(AnchorContext context, Transform target) {
        FaceSnap.orientation(face, up, target.orientation());
        return true;
    }

    @Override
    public Anchor copy() {
        return this;
    }
}
