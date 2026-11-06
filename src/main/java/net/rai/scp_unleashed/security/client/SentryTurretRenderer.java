package net.rai.scp_unleashed.security.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.security.SecurityRegistry;
import net.rai.scp_unleashed.security.SentryTurretBlock;
import net.rai.scp_unleashed.security.SentryTurretBlockEntity;
import net.rai.scp_unleashed.security.TurretGeometry;
import net.rai.scp_unleashed.security.TurretStatus;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import net.rai.scp_unleashed.tesla.client.TeslaRenderTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SentryTurretRenderer implements BlockEntityRenderer<SentryTurretBlockEntity> {
    public static final ModelResourceLocation YokeModel = ModelResourceLocation.standalone(SecurityRegistry.location("block/security/sentry_turret_yoke"));
    public static final ModelResourceLocation GunModel = ModelResourceLocation.standalone(SecurityRegistry.location("block/security/sentry_turret_gun"));
    public static final ResourceLocation GlowTexture = SecurityRegistry.location("textures/misc/security/turret_glow.png");
    public static final float PivotX = 8.0f / 16.0f;
    public static final float PivotY = 16.0f / 16.0f;
    public static final float PivotZ = 8.0f / 16.0f;
    public static final float RecoilDepth = 1.2f / 16.0f;
    public static final float[] BarrelX = {6.5f / 16.0f, 9.5f / 16.0f};
    public static final float BarrelY = 15.5f / 16.0f;
    public static final float BarrelTip = -6.0f / 16.0f;
    public static final float StarSize = 0.2f;
    public static final float PlumeLength = 0.42f;
    public static final float PlumeWidth = 0.13f;
    public static final float EyeX = 8.0f / 16.0f;
    public static final float EyeY = 17.0f / 16.0f;
    public static final float EyeZ = 1.9f / 16.0f;
    public static final float EyeHalfWidth = 1.6f / 16.0f;
    public static final float EyeHalfHeight = 1.1f / 16.0f;

    private final RandomSource Random = RandomSource.create();
    private final Map<ModelResourceLocation, List<BakedQuad>> Cached = new HashMap<>();
    private final Map<ModelResourceLocation, BakedModel> CachedModels = new HashMap<>();

    public SentryTurretRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(SentryTurretBlockEntity Turret, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        BlockState State = Turret.getBlockState();
        Level World = Turret.getLevel();
        if (World == null || !(State.getBlock() instanceof SentryTurretBlock)) {
            return;
        }
        boolean Hanging = State.getValue(SentryTurretBlock.Hanging);
        float Yaw = Turret.getYaw(PartialTick);
        float Pitch = Turret.getPitch(PartialTick);
        float Kick = Turret.getRecoil(PartialTick) * RecoilDepth;
        Pose.pushPose();
        Pose.translate(PivotX, Hanging ? TurretGeometry.CeilingPivotY : TurretGeometry.FloorPivotY, PivotZ);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Yaw));
        Pose.pushPose();
        if (Hanging) {
            Pose.mulPose(Axis.ZP.rotationDegrees(180.0f));
        }
        Pose.translate(-PivotX, -PivotY, -PivotZ);
        draw(Pose, Buffers, quads(YokeModel), PackedLight);
        Pose.popPose();
        Pose.mulPose(Axis.XP.rotationDegrees(-Pitch));
        if (Hanging) {
            Pose.mulPose(Axis.ZP.rotationDegrees(180.0f));
        }
        Pose.translate(-PivotX, -PivotY, -PivotZ + Kick);
        draw(Pose, Buffers, quads(GunModel), PackedLight);
        if (!ShaderCompat.renderingShadowPass()) {
            glow(Turret, Pose, Buffers, World, PartialTick);
        }
        Pose.popPose();
    }

    private void glow(SentryTurretBlockEntity Turret, PoseStack Pose, MultiBufferSource Buffers, Level World, float PartialTick) {
        float Flash = Turret.flash(PartialTick);
        TurretStatus Status = Turret.getStatus();
        float[] Eye = eyeColour(Status, World.getGameTime() + PartialTick);
        if (Flash <= 0.0f && Eye == null) {
            return;
        }
        VertexConsumer Consumer = Buffers.getBuffer(TeslaRenderTypes.additive(GlowTexture));
        PoseStack.Pose Last = Pose.last();
        if (Eye != null) {
            quad(Consumer, Last, EyeX - EyeHalfWidth, EyeY - EyeHalfHeight, EyeZ, EyeX + EyeHalfWidth, EyeY + EyeHalfHeight, EyeZ, 0.0f, 0.5f, 0.5f, 1.0f,
                    Eye[0], Eye[1], Eye[2], 0.0f, 0.0f, -1.0f);
        }
        if (Flash > 0.0f) {
            int Barrel = Mth.clamp(Turret.getFlashBarrel(), 0, 1);
            float X = BarrelX[Barrel];
            float Z = BarrelTip - 0.01f;
            float Size = StarSize * (0.75f + 0.25f * Flash);
            float Red = Flash;
            float Green = 0.78f * Flash;
            float Blue = 0.4f * Flash;
            quad(Consumer, Last, X - Size, BarrelY - Size, Z, X + Size, BarrelY + Size, Z, 0.0f, 0.0f, 0.5f, 0.5f, Red, Green, Blue, 0.0f, 0.0f, -1.0f);
            float Reach = PlumeLength * (0.6f + 0.4f * Flash);
            float Half = PlumeWidth * (0.7f + 0.3f * Flash);
            plume(Consumer, Last, X - Half, BarrelY, Z, X + Half, BarrelY, Z - Reach, Red, Green, Blue, 0.0f, 1.0f, 0.0f);
            plume(Consumer, Last, X, BarrelY - Half, Z, X, BarrelY + Half, Z - Reach, Red, Green, Blue, 1.0f, 0.0f, 0.0f);
        }
    }

    private static float[] eyeColour(TurretStatus Status, float Time) {
        return switch (Status) {
            case Ready -> new float[]{0.12f, 0.55f, 0.18f};
            case Tracking, Blocked -> new float[]{0.85f, 0.18f, 0.1f};
            case Firing -> new float[]{1.0f, 0.25f, 0.12f};
            case NoAmmo, Jammed -> (int) (Time / 8.0f) % 2 == 0 ? new float[]{0.85f, 0.55f, 0.1f} : null;
            default -> null;
        };
    }

    private static void quad(VertexConsumer Consumer, PoseStack.Pose Last, float X0, float Y0, float Z0, float X1, float Y1, float Z1, float U0, float V0, float U1, float V1,
                             float Red, float Green, float Blue, float NormalX, float NormalY, float NormalZ) {
        vertex(Consumer, Last, X0, Y0, Z0, U0, V1, Red, Green, Blue, NormalX, NormalY, NormalZ);
        vertex(Consumer, Last, X1, Y0, Z0, U1, V1, Red, Green, Blue, NormalX, NormalY, NormalZ);
        vertex(Consumer, Last, X1, Y1, Z1, U1, V0, Red, Green, Blue, NormalX, NormalY, NormalZ);
        vertex(Consumer, Last, X0, Y1, Z1, U0, V0, Red, Green, Blue, NormalX, NormalY, NormalZ);
    }

    private static void plume(VertexConsumer Consumer, PoseStack.Pose Last, float X0, float Y0, float Z0, float X1, float Y1, float Z1, float Red, float Green, float Blue,
                              float NormalX, float NormalY, float NormalZ) {
        vertex(Consumer, Last, X0, Y0, Z0, 0.5f, 0.5f, Red, Green, Blue, NormalX, NormalY, NormalZ);
        vertex(Consumer, Last, X1, Y1, Z0, 0.5f, 0.0f, Red, Green, Blue, NormalX, NormalY, NormalZ);
        vertex(Consumer, Last, X1, Y1, Z1, 1.0f, 0.0f, Red, Green, Blue, NormalX, NormalY, NormalZ);
        vertex(Consumer, Last, X0, Y0, Z1, 1.0f, 0.5f, Red, Green, Blue, NormalX, NormalY, NormalZ);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, float Red, float Green, float Blue,
                               float NormalX, float NormalY, float NormalZ) {
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(Red, Green, Blue, 1.0f)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, NormalX, NormalY, NormalZ);
    }

    private void draw(PoseStack Pose, MultiBufferSource Buffers, List<BakedQuad> Quads, int Light) {
        if (Quads.isEmpty()) {
            return;
        }
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose Last = Pose.last();
        for (BakedQuad Quad : Quads) {
            Consumer.putBulkData(Last, Quad, 1.0f, 1.0f, 1.0f, 1.0f, Light, OverlayTexture.NO_OVERLAY);
        }
    }

    private List<BakedQuad> quads(ModelResourceLocation Location) {
        BakedModel Baked = Minecraft.getInstance().getModelManager().getModel(Location);
        if (CachedModels.get(Location) != Baked) {
            CachedModels.put(Location, Baked);
            List<BakedQuad> Found = new ArrayList<>();
            for (Direction Side : Direction.values()) {
                Random.setSeed(42L);
                Found.addAll(Baked.getQuads(null, Side, Random, ModelData.EMPTY, null));
            }
            Random.setSeed(42L);
            Found.addAll(Baked.getQuads(null, null, Random, ModelData.EMPTY, null));
            Cached.put(Location, Found);
        }
        return Cached.getOrDefault(Location, List.of());
    }

    @Override
    public boolean shouldRenderOffScreen(SentryTurretBlockEntity Turret) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(SentryTurretBlockEntity Turret) {
        return Turret.getRenderBounds();
    }
}
