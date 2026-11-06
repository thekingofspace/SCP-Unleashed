package net.rai.scp_unleashed.scp427.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public final class Scp427Convulsion {
    public static final float Shift = 0.035f;
    public static final float ShiftGrowth = 0.05f;
    public static final float Roll = 6.0f;
    public static final float RollGrowth = 9.0f;
    public static final float Pitch = 4.0f;
    public static final float PitchGrowth = 6.0f;
    public static final float Swell = 0.2f;
    public static final float Rise = 0.1f;
    private static final Map<Integer, float[]> Applied = new HashMap<>();

    private Scp427Convulsion() {
    }

    public static void apply(Entity Subject, PoseStack Pose, float Partial) {
        float Amount = Scp427ClientState.convulsion(Subject.getId(), Partial);
        if (Amount <= 0.0f) {
            return;
        }
        float Clock = Subject.tickCount + Partial;
        float Seed = Subject.getId() * 0.731f;
        float Grow = Amount * Amount * (3.0f - 2.0f * Amount);
        float Size = Shift + ShiftGrowth * Amount;
        float Dx = Mth.sin(Clock * 2.9f + Seed) * Size;
        float Dy = Math.abs(Mth.sin(Clock * 3.7f + Seed * 2.0f)) * Size;
        float Dz = Mth.cos(Clock * 3.1f + Seed) * Size;
        float Turn = Mth.sin(Clock * 2.3f + Seed) * (Roll + RollGrowth * Amount);
        float Tip = Mth.sin(Clock * 3.4f + Seed + 1.0f) * (Pitch + PitchGrowth * Amount);
        float Wide = 1.0f + Swell * Grow;
        float Tall = 1.0f + Rise * Grow;
        float Half = Subject.getBbHeight() * 0.5f;
        Pose.translate(Dx, Half + Dy, Dz);
        Pose.mulPose(Axis.ZP.rotationDegrees(Turn));
        Pose.mulPose(Axis.XP.rotationDegrees(Tip));
        Pose.scale(Wide, Tall, Wide);
        Pose.translate(0.0f, -Half, 0.0f);
        Applied.put(Subject.getId(), new float[]{Dx, Dy, Dz, Turn, Tip, Wide, Tall, Half});
    }

    public static void undo(Entity Subject, PoseStack Pose) {
        float[] Values = Applied.remove(Subject.getId());
        if (Values == null) {
            return;
        }
        Pose.translate(0.0f, Values[7], 0.0f);
        Pose.scale(1.0f / Values[5], 1.0f / Values[6], 1.0f / Values[5]);
        Pose.mulPose(Axis.XP.rotationDegrees(-Values[4]));
        Pose.mulPose(Axis.ZP.rotationDegrees(-Values[3]));
        Pose.translate(-Values[0], -Values[7] - Values[1], -Values[2]);
    }

    public static void forget(Entity Subject) {
        Applied.remove(Subject.getId());
    }

    public static void reset() {
        Applied.clear();
    }
}
