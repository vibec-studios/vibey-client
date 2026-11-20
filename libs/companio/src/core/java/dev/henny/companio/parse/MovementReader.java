package dev.henny.companio.parse;

import com.google.gson.JsonObject;
import dev.henny.companio.def.FollowDef;
import dev.henny.companio.def.LookDef;
import dev.henny.companio.def.MovementDef;
import dev.henny.companio.pack.Problems;
import dev.henny.cosmetico.model.Bone;
import dev.henny.cosmetico.model.Bundle;

final class MovementReader {
    private MovementReader() {}

    static MovementDef read(JsonObject root, Problems p) {
        MovementDef d = MovementDef.DEFAULT;
        JsonObject o = Json.obj(root, "movement", "", p);
        if (o == null) return d;
        String at = "movement";
        Json.known(o, at, p, "walkSpeed", "runSpeed", "swimSpeed", "flySpeed", "canSwim", "canFly", "jump", "step",
                "turnSpeed", "follow", "wander", "shoulder", "head");
        float walk = Json.num(o, "walkSpeed", at, p, d.walkSpeed(), 0.2f, 12f);
        float run = Json.num(o, "runSpeed", at, p, Math.max(walk, d.runSpeed()), 0.2f, 16f);
        boolean canFly = Json.bool(o, "canFly", at, p, false);
        float fly = Json.num(o, "flySpeed", at, p, canFly ? run : 0f, 0f, 16f);
        if (canFly && fly <= 0f) p.error(Json.at(at, "flySpeed"), "canFly needs a flySpeed above 0");
        return new MovementDef(walk, Math.max(walk, run), Json.num(o, "swimSpeed", at, p, d.swimSpeed(), 0f, 12f), fly,
                Json.bool(o, "canSwim", at, p, true), canFly, Json.num(o, "jump", at, p, d.jump(), 0f, 3f),
                Json.num(o, "step", at, p, d.step(), 0f, 1f), Json.num(o, "turnSpeed", at, p, d.turnSpeed(), 30f, 1440f),
                follow(o, p), Json.num(o, "wander", at, p, d.wander(), 1f, 16f),
                Json.vec3(o, "shoulder", at, p, d.shoulder(), 4f), Json.vec3(o, "head", at, p, d.head(), 4f));
    }

    private static FollowDef follow(JsonObject m, Problems p) {
        FollowDef d = FollowDef.DEFAULT;
        JsonObject o = Json.obj(m, "follow", "movement", p);
        if (o == null) return d;
        String at = "movement.follow";
        Json.known(o, at, p, "near", "far", "run", "teleport");
        float near = Json.num(o, "near", at, p, d.near(), 0.5f, 8f);
        float far = Json.num(o, "far", at, p, Math.max(near + 1f, d.far()), 1f, 16f);
        float run = Json.num(o, "run", at, p, Math.max(far, d.run()), 1f, 32f);
        float tp = Json.num(o, "teleport", at, p, Math.max(run + 4f, d.teleport()), 8f, 64f);
        if (far <= near) p.error(Json.at(at, "far"), "must be bigger than near");
        return new FollowDef(near, Math.max(far, near + 0.5f), Math.max(run, far), Math.max(tp, run));
    }

    static LookDef look(JsonObject root, Bundle model, Problems p) {
        JsonObject o = Json.obj(root, "look", "", p);
        if (o == null) return model != null && model.model.bone("head") != null ? new LookDef("head", 50f, 30f, 240f) : LookDef.NONE;
        Json.known(o, "look", p, "bone", "maxYaw", "maxPitch", "speed");
        String bone = Json.str(o, "bone", "look", p, null);
        if (bone != null && model != null && model.model.bone(bone) == null) {
            p.error("look.bone", "the model has no bone \"" + bone + "\"" + BoneNames.hint(model, bone));
            bone = null;
        }
        return new LookDef(bone, Json.num(o, "maxYaw", "look", p, 50f, 0f, 180f), Json.num(o, "maxPitch", "look", p, 30f, 0f, 90f),
                Json.num(o, "speed", "look", p, 240f, 10f, 1440f));
    }

    static int boneIndex(Bundle model, String bone) {
        if (model == null || bone == null) return -1;
        Bone b = model.model.bone(bone);
        return b == null ? -1 : b.index;
    }
}
