package dev.henny.companio.brain;

public final class Self {
    public double speed;
    public boolean moving, onGround = true, inWater, onIce, onSnow, held, riding;
    public String command = "follow", variant = "default";
    public double size = 1, age, x, y, z;
    public boolean sky;
}
