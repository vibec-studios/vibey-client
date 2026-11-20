package dev.henny.r3.anchor;

import org.joml.Vector3dc;

import java.util.UUID;

public interface WorldQuery {
    String dimension();

    boolean raycastBlocks(Vector3dc origin, Vector3dc direction, double maxDistance, BlockHit out);

    boolean entity(UUID id, EntityPose out);

    int lightAt(double x, double y, double z);
}
