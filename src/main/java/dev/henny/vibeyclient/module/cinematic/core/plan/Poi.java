package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.V3;

public record Poi(V3 focus, double score, int built, int lights, int variety, int relief, boolean water) {}
