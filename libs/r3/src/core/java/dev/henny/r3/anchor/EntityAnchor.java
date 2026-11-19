package dev.henny.r3.anchor;

import dev.henny.r3.object.Transform;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.UUID;

public final class EntityAnchor implements Anchor {
    public static final float SNEAK_DROP = 0.25f;
    private final UUID entity;
    private final Vector3d offset;
    private final boolean aboveHead;
    private final Facing facing;
    private final EntityPose pose = new EntityPose();

    public EntityAnchor(UUID entity, Vector3dc offset, boolean aboveHead, Facing facing) {
        this.entity = entity;
        this.offset = new Vector3d(offset);
        this.aboveHead = aboveHead;
        this.facing = facing;
    }

    public static EntityAnchor aboveHead(UUID entity, double extraHeight) {
        return new EntityAnchor(entity, new Vector3d(0, extraHeight, 0), true, Facing.CAMERA_YAW);
    }

    public UUID entity() {
        return entity;
    }

    public Vector3dc offset() {
        return offset;
    }

    public boolean aboveHead() {
        return aboveHead;
    }

    public Facing facing() {
        return facing;
    }

    @Override
    public AnchorKind kind() {
        return AnchorKind.ENTITY;
    }

    @Override
    public boolean resolve(AnchorContext context, Transform target) {
        if (context.query() == null || !context.query().entity(entity, pose)) return false;
        double head = aboveHead ? pose.height() - (pose.sneaking() ? SNEAK_DROP : 0f) : 0;
        target.position().set(pose.position()).add(offset.x, offset.y + head, offset.z);
        if (context.camera() != null) facing.apply(target, context.camera(), pose.bodyYaw());
        return true;
    }

    @Override
    public Anchor copy() {
        return new EntityAnchor(entity, offset, aboveHead, facing);
    }

    @Override
    public boolean followsSomething() {
        return true;
    }
}
