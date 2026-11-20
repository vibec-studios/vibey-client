package dev.henny.vibeyclient.module.visuals.items;

public interface ItemPose {
    void vibey$pose(boolean active, float yaw, float rotX, float lift, float centerY, float centerZ);

    boolean vibey$active();

    float vibey$yaw();

    float vibey$rotX();

    float vibey$lift();

    float vibey$centerY();

    float vibey$centerZ();
}
