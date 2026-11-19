package dev.henny.r3.view;

@FunctionalInterface
public interface ViewFrustum {
    ViewFrustum ALL = (minX, minY, minZ, maxX, maxY, maxZ) -> true;

    boolean isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
