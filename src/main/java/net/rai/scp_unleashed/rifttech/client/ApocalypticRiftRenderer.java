package net.rai.scp_unleashed.rifttech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.client.RiftRenderer;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

import java.util.ArrayList;
import java.util.List;

public class ApocalypticRiftRenderer extends RiftRenderer {
    public static final double FineReach = 48.0;
    public static final double CoarseReach = 112.0;
    private static final float[] Gold = {1.0f, 0.76f, 0.24f};
    private static final float[] Hot = {1.0f, 0.93f, 0.7f};
    private static final float[] White = {1.0f, 0.99f, 0.9f};

    public ApocalypticRiftRenderer(EntityRendererProvider.Context Context) {
        super(Context);
    }

    @Override
    public void render(RiftEntity Rift, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        Pose.pushPose();
        Pose.scale(GoldenFracture.HeartScale, GoldenFracture.HeartScale, GoldenFracture.HeartScale);
        super.render(Rift, Yaw, PartialTick, Pose, Buffers, PackedLight);
        Pose.popPose();
        if (!(Rift instanceof ApocalypticRiftEntity Golden) || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Open = Rift.getOpenness(PartialTick);
        float Collapse = Rift.getCollapse(PartialTick);
        if (Open <= 0.01f || Collapse >= 0.99f) {
            return;
        }
        float Extraction = Rift.getShownExtraction(PartialTick);
        Vec3 Origin = Rift.getPosition(PartialTick);
        double Lift = Rift.getRiftHeight() * 0.5 * GoldenFracture.HeartScale;
        Vec3 Eye = entityRenderDispatcher.camera.getPosition().subtract(Origin).subtract(0.0, Lift, 0.0);
        double Distance = Eye.length();
        double Time = (Rift.level().getGameTime() % 1000000L) + PartialTick;
        float Scale = (float) (Math.sqrt(Open) * (1.0 - Collapse * Collapse));
        GoldenFracture.Fracture Shape = GoldenFracture.build(Golden.getSeed(), Time, GoldenFracture.levelsFor(Distance), Distance < FineReach, Scale, Extraction);
        List<GoldenFracture.Branch> Branches = Shape.Branches();
        float Pulse = 0.85f + 0.15f * Mth.sin((float) Time * 0.27f) + 0.35f * Extraction + 0.8f * Collapse;
        Pose.pushPose();
        Pose.translate(0.0, Lift, 0.0);
        PoseStack.Pose Saved = Pose.last().copy();
        Pose.popPose();
        RiftTechDeferred.queue(Own -> fracture(Own, Saved, Golden, Branches, Pulse, Eye, Distance, Origin, Lift, PartialTick, true));
        RiftTechDeferred.queueLate(Own -> fracture(Own, Saved, Golden, Branches, Pulse, Eye, Distance, Origin, Lift, PartialTick, false));
    }

    private static void fracture(MultiBufferSource Buffers, PoseStack.Pose Last, ApocalypticRiftEntity Golden, List<GoldenFracture.Branch> Branches,
                                 float Pulse, Vec3 Eye, double Distance, Vec3 Origin, double Lift, float PartialTick, boolean CorePass) {
        if (!CorePass) {
            VertexConsumer Halo = Buffers.getBuffer(RiftTechRenderTypes.Arc);
            for (int Index = 1; Index < Branches.size(); Index++) {
                GoldenFracture.Branch Item = Branches.get(Index);
                float Glow = 0.6f * Item.Heat() * Pulse;
                Ribbons.ribbon(Halo, Last, Item.Points(), Eye, Item.Half() * 3.0f, 0.3f, Gold[0] * Glow, Gold[1] * Glow, Gold[2] * Glow, 1.0f);
            }
            bolts(Halo, Last, Golden, Branches, Origin.add(0.0, Lift, 0.0), Eye, PartialTick, false);
            return;
        }
        VertexConsumer Core = Buffers.getBuffer(RiftTechRenderTypes.ArcCore);
        if (Distance < CoarseReach) {
            for (int Index = 1; Index < Branches.size(); Index++) {
                GoldenFracture.Branch Item = Branches.get(Index);
                float Heat = Math.min(1.6f, Item.Heat() * Pulse);
                Ribbons.ribbon(Core, Last, Item.Points(), Eye, Item.Half(), 0.25f, Hot[0] * Heat, Hot[1] * Heat, Hot[2] * Heat, 1.0f);
                Ribbons.ribbon(Core, Last, Item.Points(), Eye, Item.Half() * 0.45f, 0.25f, White[0] * Heat * 0.8f, White[1] * Heat * 0.8f, White[2] * Heat * 0.8f, 1.0f);
            }
        }
        bolts(Core, Last, Golden, Branches, Origin.add(0.0, Lift, 0.0), Eye, PartialTick, true);
    }

    private static void bolts(VertexConsumer Consumer, PoseStack.Pose Last, ApocalypticRiftEntity Golden, List<GoldenFracture.Branch> Branches, Vec3 Nexus, Vec3 Eye, float PartialTick, boolean CorePass) {
        long Now = Golden.level().getGameTime();
        for (ApocalypticRiftEntity.Bolt Strike : new ArrayList<>(Golden.getBolts())) {
            float Age = Now - Strike.Birth() + PartialTick;
            if (Age < 0.0f || Age > ApocalypticRiftEntity.BoltTicks) {
                continue;
            }
            float Strength = (float) Math.pow(1.0f - Age / ApocalypticRiftEntity.BoltTicks, 0.6);
            if (Age >= 2.0f && Age < 3.0f) {
                Strength *= 0.25f;
            }
            Vec3 Target = Strike.Target().subtract(Nexus);
            Vec3 Start = GoldenFracture.strikeOrigin(Branches, Target);
            RandomSource Random = RandomSource.create(Strike.Seed() * 7919L + (long) (Age / 2.0f));
            List<Vec3> Path = Ribbons.path(Random, Start, Target, 6, 0.13);
            if (!CorePass) {
                Ribbons.ribbon(Consumer, Last, Path, Eye, 0.42f, 1.0f, Gold[0] * 0.55f * Strength, Gold[1] * 0.55f * Strength, Gold[2] * 0.55f * Strength, 1.0f);
                continue;
            }
            Ribbons.ribbon(Consumer, Last, Path, Eye, 0.12f, 1.0f, Hot[0] * Strength, Hot[1] * Strength, Hot[2] * Strength, 1.0f);
            Ribbons.ribbon(Consumer, Last, Path, Eye, 0.05f, 1.0f, White[0] * Strength, White[1] * Strength, White[2] * Strength, 1.0f);
            for (int Fork = 0; Fork < 2; Fork++) {
                Vec3 Root = Path.get(8 + Random.nextInt(Math.max(1, Path.size() - 20)));
                Vec3 Ahead = Target.subtract(Root);
                Vec3 Drift = new Vec3(Random.nextDouble() - 0.5, Random.nextDouble() - 0.6, Random.nextDouble() - 0.5).scale(Ahead.length() * 0.6);
                Vec3 Tip = Root.add(Ahead.scale(0.25 + 0.2 * Random.nextDouble())).add(Drift);
                List<Vec3> Twig = Ribbons.path(Random, Root, Tip, 4, 0.2);
                Ribbons.ribbon(Consumer, Last, Twig, Eye, 0.05f, 0.2f, White[0] * Strength * 0.7f, White[1] * Strength * 0.7f, White[2] * Strength * 0.7f, 1.0f);
            }
        }
    }
}
