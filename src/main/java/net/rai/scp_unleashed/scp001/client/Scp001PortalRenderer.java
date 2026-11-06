package net.rai.scp_unleashed.scp001.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.rifttech.client.GoldenFracture;
import net.rai.scp_unleashed.rifttech.client.Ribbons;
import net.rai.scp_unleashed.rifttech.client.RiftTechRenderTypes;
import net.rai.scp_unleashed.scp001.Scp001Portal;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

import java.util.Arrays;
import java.util.List;

public class Scp001PortalRenderer extends EntityRenderer<Scp001Portal> {
    public static final float PortalScale = 0.27f;
    public static final double Lift = 0.05;
    public static final double FineReach = 32.0;
    private static final float[] Gold = {1.0f, 0.76f, 0.24f};
    private static final float[] Hot = {1.0f, 0.93f, 0.7f};
    private static final float[] White = {1.0f, 0.99f, 0.9f};

    public Scp001PortalRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Scp001Portal Portal, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Open = Portal.openness(PartialTick);
        if (Open <= 0.01f) {
            return;
        }
        Vec3 Origin = Portal.getPosition(PartialTick);
        Vec3 Eye = entityRenderDispatcher.camera.getPosition().subtract(Origin).subtract(0.0, Lift, 0.0);
        double Distance = Eye.length();
        double Time = (Portal.level().getGameTime() % 1000000L) + PartialTick;
        float Scale = (float) (PortalScale * Math.sqrt(Open));
        GoldenFracture.Fracture Shape = GoldenFracture.build(Portal.getSeed(), Time * 3.0, GoldenFracture.levelsFor(Distance * 3.0), Distance < FineReach, Scale, 0.4f);
        List<GoldenFracture.Branch> Branches = Shape.Branches();
        float Pulse = 0.9f + 0.2f * Mth.sin((float) Time * 0.6f);
        Pose.pushPose();
        Pose.translate(0.0, Lift, 0.0);
        PoseStack.Pose Last = Pose.last();
        List<Vec3> Trunk = Shape.trunk().Points();
        int Start = Math.max(0, Shape.TearStart() - 1);
        boolean Torn = Start < Trunk.size() - 1;
        List<Vec3> Tear = Torn ? Trunk.subList(Start, Trunk.size()) : List.of();
        float[] Halves = Torn ? Arrays.copyOfRange(Shape.Tear(), Start, Trunk.size()) : new float[0];
        VertexConsumer Halo = Buffers.getBuffer(RiftTechRenderTypes.Arc);
        if (Torn) {
            float Glow = 0.65f * Pulse;
            Ribbons.ribbon(Halo, Last, Tear, Eye, Ribbons.widths(Halves, 1.6f, 0.3f * Scale), Gold[0] * Glow, Gold[1] * Glow, Gold[2] * Glow, 1.0f);
        }
        for (int Index = 1; Index < Branches.size(); Index++) {
            GoldenFracture.Branch Item = Branches.get(Index);
            float Glow = 0.6f * Item.Heat() * Pulse;
            Ribbons.ribbon(Halo, Last, Item.Points(), Eye, Item.Half() * 3.0f, 0.3f, Gold[0] * Glow, Gold[1] * Glow, Gold[2] * Glow, 1.0f);
        }
        if (Torn) {
            VertexConsumer Void = Buffers.getBuffer(RiftTechRenderTypes.Void);
            Ribbons.ribbon(Void, Last, Tear, Eye, Halves, 0.06f, 0.035f, 0.01f, 0.94f);
        }
        VertexConsumer Core = Buffers.getBuffer(RiftTechRenderTypes.Arc);
        if (Torn) {
            float[] Edge = Ribbons.widths(Halves, 0.92f, 0.0f);
            for (float Sign : new float[]{-1.0f, 1.0f}) {
                List<Vec3> Rim = Ribbons.offset(Tear, Eye, Edge, Sign);
                Ribbons.ribbon(Core, Last, Rim, Eye, 0.16f * Scale, 0.4f, Hot[0] * Pulse, Hot[1] * Pulse, Hot[2] * Pulse, 1.0f);
                Ribbons.ribbon(Core, Last, Rim, Eye, 0.06f * Scale, 0.4f, White[0] * Pulse * 0.8f, White[1] * Pulse * 0.8f, White[2] * Pulse * 0.8f, 1.0f);
            }
        }
        for (int Index = 1; Index < Branches.size(); Index++) {
            GoldenFracture.Branch Item = Branches.get(Index);
            float Heat = Math.min(1.6f, Item.Heat() * Pulse);
            Ribbons.ribbon(Core, Last, Item.Points(), Eye, Item.Half(), 0.25f, Hot[0] * Heat, Hot[1] * Heat, Hot[2] * Heat, 1.0f);
        }
        Pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Scp001Portal Portal) {
        return RiftTechRenderTypes.ArcTexture;
    }
}
