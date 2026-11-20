package dev.henny.companio.world;

import dev.henny.companio.anim.Animator;
import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.move.Body;
import dev.henny.cosmetico.model.Bundle;

public interface CompanionView {
    CompanionDef def();

    Bundle model();

    Body body();

    Animator animator();

    float headYaw(float partial);

    float headPitch(float partial);

    boolean[] hiddenBones(boolean[] out);

    String bubbleText();

    String name();

    float size();

    boolean held();
}
