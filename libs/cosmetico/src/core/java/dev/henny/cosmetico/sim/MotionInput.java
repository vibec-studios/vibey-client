package dev.henny.cosmetico.sim;

public record MotionInput(float walkSpeed, boolean flying, boolean fallFlying, double x, double y, double z, boolean onGround,
                          boolean inWater, boolean crouching, boolean sprinting, boolean swimming, float bodyYaw) {}
