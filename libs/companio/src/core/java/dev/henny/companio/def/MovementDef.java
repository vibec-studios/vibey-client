package dev.henny.companio.def;

public record MovementDef(float walkSpeed, float runSpeed, float swimSpeed, float flySpeed, boolean canSwim, boolean canFly,
                          float jump, float step, float turnSpeed, FollowDef follow, float wander, float[] shoulder, float[] head) {
    public static final MovementDef DEFAULT = new MovementDef(3.2f, 6.5f, 2.6f, 0f, true, false, 1.1f, 0.6f, 300f,
            FollowDef.DEFAULT, 5f, new float[] {0.42f, 1.38f, 0.05f}, new float[] {0f, 1.95f, 0f});
}
