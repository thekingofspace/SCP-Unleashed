package net.rai.scp_unleashed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.wire.WireTier;

import java.util.EnumMap;
import java.util.Map;

public final class WireRenderer {
    private static final float SideWidthU = 0.25f;
    private static final Map<WireTier, RenderType> Types = new EnumMap<>(WireTier.class);

    private WireRenderer() {
    }

    public static RenderType typeFor(WireTier Tier) {
        return Types.computeIfAbsent(Tier, Key -> RenderType.entityCutoutNoCull(
                ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/" + Key.getTextureName() + ".png")));
    }

    public static int anchorLight(BlockAndTintGetter World, BlockPos Pos, Direction Face, boolean FullBlock) {
        return LevelRenderer.getLightColor(World, FullBlock ? Pos.relative(Face) : Pos);
    }

    public static void render(PoseStack Pose, MultiBufferSource Buffers, Vec3 From, Vec3 To, int LightFrom, int LightTo, int Colour, WireTier Tier, double HalfWidth) {
        Vec3 Span = To.subtract(From);
        double Length = Span.length();
        if (Length < 1.0E-4) {
            return;
        }
        Vec3 Forward = Span.scale(1.0 / Length);
        Vec3 Up = Math.abs(Forward.y) > 0.99 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 Side = Forward.cross(Up).normalize();
        Vec3 Lift = Side.cross(Forward).normalize();
        Vec3[] Axes = {Lift, Side, Lift.scale(-1.0), Side.scale(-1.0)};
        VertexConsumer Consumer = Buffers.getBuffer(typeFor(Tier));
        PoseStack.Pose Last = Pose.last();
        float LengthV = (float) Length;
        float CapHeightV = (float) (Tier.getHalfWidth() * 2.0);
        int StartLight = glowing(LightFrom, Tier.getGlow());
        int EndLight = glowing(LightTo, Tier.getGlow());
        for (int Face = 0; Face < 4; Face++) {
            Vec3 Normal = Axes[Face];
            Vec3 Edge = Axes[(Face + 1) % 4].scale(HalfWidth);
            Vec3 Offset = Normal.scale(HalfWidth);
            float MinU = Face * SideWidthU;
            float MaxU = MinU + SideWidthU;
            vertex(Consumer, Last, From.add(Offset).subtract(Edge), MinU, 0.0f, Colour, StartLight, Normal);
            vertex(Consumer, Last, From.add(Offset).add(Edge), MaxU, 0.0f, Colour, StartLight, Normal);
            vertex(Consumer, Last, To.add(Offset).add(Edge), MaxU, LengthV, Colour, EndLight, Normal);
            vertex(Consumer, Last, To.add(Offset).subtract(Edge), MinU, LengthV, Colour, EndLight, Normal);
        }
        cap(Consumer, Last, From, Forward.scale(-1.0), Side, Lift, HalfWidth, CapHeightV, Colour, StartLight);
        cap(Consumer, Last, To, Forward, Side, Lift, HalfWidth, CapHeightV, Colour, EndLight);
    }

    private static int glowing(int Light, int Glow) {
        return Glow <= 0 ? Light : LightTexture.pack(Math.max(LightTexture.block(Light), Glow), LightTexture.sky(Light));
    }

    private static void cap(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, Vec3 Normal, Vec3 Side, Vec3 Lift, double HalfWidth, float CapHeightV, int Colour, int Light) {
        Vec3 Across = Side.scale(HalfWidth);
        Vec3 Over = Lift.scale(HalfWidth);
        vertex(Consumer, Last, Centre.subtract(Across).subtract(Over), 0.0f, 0.0f, Colour, Light, Normal);
        vertex(Consumer, Last, Centre.add(Across).subtract(Over), SideWidthU, 0.0f, Colour, Light, Normal);
        vertex(Consumer, Last, Centre.add(Across).add(Over), SideWidthU, CapHeightV, Colour, Light, Normal);
        vertex(Consumer, Last, Centre.subtract(Across).add(Over), 0.0f, CapHeightV, Colour, Light, Normal);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Point, float U, float V, int Colour, int Light, Vec3 Normal) {
        Consumer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z)
                .setColor(Colour)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Light)
                .setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }
}
