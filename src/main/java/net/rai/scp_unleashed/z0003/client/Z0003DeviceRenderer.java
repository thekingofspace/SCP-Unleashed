package net.rai.scp_unleashed.z0003.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import net.rai.scp_unleashed.z0003.Z0003Device;
import org.joml.Matrix4f;

public class Z0003DeviceRenderer extends EntityRenderer<Z0003Device> {
    public static final float ModelScale = 0.17f;
    public static final float RestLift = 0.1f;
    public static final float HoverBob = 0.04f;
    public static final float HoverTilt = 14.0f;
    public static final float FieldRed = 0.36f;
    public static final float FieldGreen = 1.0f;
    public static final float FieldBlue = 0.48f;
    public static final float HaloSize = 0.9f;
    public static final float FlashTicks = 8.0f;
    public static final float FlashStrength = 0.9f;
    public static final int RopeSegments = 24;
    public static final int MaxRopeSegments = 96;
    public static final float RopeDetail = 1.5f;
    public static final float RopeWidth = 0.03f;
    public static final float RopeRed = 0.46f;
    public static final float RopeGreen = 0.44f;
    public static final float RopeBlue = 0.4f;

    private final ItemRenderer Items;

    public Z0003DeviceRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        Items = Context.getItemRenderer();
        shadowRadius = 0.2f;
        shadowStrength = 0.6f;
    }

    @Override
    public void render(Z0003Device Device, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        boolean Active = Device.isActive();
        boolean Shadow = ShaderCompat.renderingShadowPass();
        float Age = Device.tickCount + Partial;
        ItemStack Stack = Device.shown();
        Pose.pushPose();
        Pose.translate(0.0f, Z0003Device.Height * 0.5f, 0.0f);
        float Spin = Mth.lerp(Partial, Device.SpinO, Device.Spin);
        if (Active) {
            Pose.translate(0.0f, Mth.sin(Age * 0.12f) * HoverBob, 0.0f);
            Pose.mulPose(Axis.YP.rotationDegrees(Spin));
            Pose.mulPose(Axis.XP.rotationDegrees(HoverTilt));
        } else if (Device.onGround()) {
            Pose.translate(0.0f, -Z0003Device.Height * 0.5f + RestLift, 0.0f);
            Pose.mulPose(Axis.YP.rotationDegrees(-Device.getYRot()));
            Pose.mulPose(Axis.XP.rotationDegrees(90.0f));
        } else {
            Pose.mulPose(Axis.YP.rotationDegrees(-Device.getYRot()));
            Pose.mulPose(Axis.XP.rotationDegrees(Spin));
        }
        Pose.scale(ModelScale, ModelScale, ModelScale);
        float Pulse = 0.0f;
        BakedModel Model;
        if (Active) {
            Model = Minecraft.getInstance().getModelManager().getModel(Z0003Glow.ActiveModel);
        } else if (Device.isArmed()) {
            Pulse = Z0003Glow.pulse(Device.due() - Device.level().getGameTime() - Partial);
            Model = Minecraft.getInstance().getModelManager().getModel(Z0003Glow.GlowModels.get(Z0003Glow.level(Pulse) - 1));
        } else {
            Model = Items.getModel(Stack, Device.level(), null, Device.getId());
        }
        Items.render(Stack, ItemDisplayContext.NONE, false, Pose, Buffers, Light, OverlayTexture.NO_OVERLAY, Model);
        Pose.popPose();
        if (!Shadow && Device.isArmed()) {
            halo(Pose, Buffers, Age, 0.15f + 0.35f * Pulse, 0.6f);
        }
        if (Active && !Shadow) {
            float Fade = Device.fieldFade(Partial);
            float Elapsed = Device.level().getGameTime() - Device.fieldStart() + Partial;
            float Since = Elapsed - Z0003Device.growTicks(Device.radius());
            float Flash = Since >= 0.0f && Since < FlashTicks ? (1.0f - Since / FlashTicks) * FlashStrength : 0.0f;
            halo(Pose, Buffers, Age, (0.75f + Flash) * Fade, 1.0f + Flash * 1.5f);
            float Reach = Device.fieldReach(Partial);
            if (Reach > 0.05f && Fade > 0.0f && entityRenderDispatcher.camera != null) {
                Vec3 Centre = Device.getPosition(Partial).add(0.0, Z0003Device.Height * 0.5, 0.0);
                float Grow = Device.radius() <= 0.0f ? 1.0f : Mth.clamp(Reach / Device.radius(), 0.0f, 1.0f);
                Z0003FieldLayers.Look Look = new Z0003FieldLayers.Look(Reach, Fade, Grow, Flash, Grow < 1.0f ? 1.0f : 0.0f, Math.max(0.0f, Elapsed));
                if (ShaderCompat.shaderPackInUse()) {
                    Pose.pushPose();
                    Pose.translate(0.0f, Z0003Device.Height * 0.5f, 0.0f);
                    Z0003FieldLayers.render(Pose, Buffers, Look, entityRenderDispatcher.camera.getPosition().subtract(Centre), entityRenderDispatcher.cameraOrientation());
                    Pose.popPose();
                } else {
                    Z0003FieldPass.queue(Centre, Look);
                }
            }
        }
        Entity Holder = Device.tetherHolder();
        if (Holder != null && !Shadow) {
            rope(Device, Partial, Pose, Buffers, Holder);
        }
        super.render(Device, Yaw, Partial, Pose, Buffers, Light);
    }

    private void halo(PoseStack Pose, MultiBufferSource Buffers, float Age, float Strength, float Grow) {
        float Pulse = 0.85f + 0.15f * Mth.sin(Age * 0.3f);
        float Size = HaloSize * Pulse * Grow;
        Pose.pushPose();
        Pose.translate(0.0f, Z0003Device.Height * 0.5f, 0.0f);
        Pose.mulPose(entityRenderDispatcher.cameraOrientation());
        PoseStack.Pose Last = Pose.last();
        VertexConsumer Consumer = Buffers.getBuffer(Z0003RenderTypes.glow(Z0003RenderTypes.GlowTexture));
        int[] Colour = colour(Mth.clamp(Strength, 0.0f, 1.0f));
        vertex(Consumer, Last, -Size, -Size, 0.0f, 0.0f, 1.0f, Colour, 0.0f, 0.0f, 1.0f);
        vertex(Consumer, Last, Size, -Size, 0.0f, 1.0f, 1.0f, Colour, 0.0f, 0.0f, 1.0f);
        vertex(Consumer, Last, Size, Size, 0.0f, 1.0f, 0.0f, Colour, 0.0f, 0.0f, 1.0f);
        vertex(Consumer, Last, -Size, Size, 0.0f, 0.0f, 0.0f, Colour, 0.0f, 0.0f, 1.0f);
        Pose.popPose();
    }

    private static int[] colour(float Strength) {
        if (Z0003RenderTypes.additive()) {
            return new int[]{(int) (FieldRed * Strength * 255.0f), (int) (FieldGreen * Strength * 255.0f), (int) (FieldBlue * Strength * 255.0f), 255};
        }
        return new int[]{(int) (FieldRed * 255.0f), (int) (FieldGreen * 255.0f), (int) (FieldBlue * 255.0f), (int) (Strength * 255.0f)};
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, int[] Colour, float Nx, float Ny, float Nz) {
        Consumer.addVertex(Last, X, Y, Z).setColor(Colour[0], Colour[1], Colour[2], Colour[3]).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(Last, Nx, Ny, Nz);
    }

    private void rope(Z0003Device Device, float Partial, PoseStack Pose, MultiBufferSource Buffers, Entity Holder) {
        Pose.pushPose();
        Vec3 Hold = Holder.getRopeHoldPosition(Partial);
        double StartX = Mth.lerp(Partial, Device.xo, Device.getX());
        double StartY = Mth.lerp(Partial, Device.yo, Device.getY()) + Z0003Device.Height * 0.5;
        double StartZ = Mth.lerp(Partial, Device.zo, Device.getZ());
        Pose.translate(0.0, Z0003Device.Height * 0.5, 0.0);
        float Dx = (float) (Hold.x - StartX);
        float Dy = (float) (Hold.y - StartY);
        float Dz = (float) (Hold.z - StartZ);
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.leash());
        Matrix4f Matrix = Pose.last().pose();
        int Segments = Mth.clamp(Mth.ceil(Mth.sqrt(Dx * Dx + Dy * Dy + Dz * Dz) * RopeDetail), RopeSegments, MaxRopeSegments);
        float Across = Mth.invSqrt(Dx * Dx + Dz * Dz) * RopeWidth / 2.0f;
        float SideX = Dz * Across;
        float SideZ = Dx * Across;
        BlockPos DevicePos = BlockPos.containing(Device.getEyePosition(Partial));
        BlockPos HolderPos = BlockPos.containing(Holder.getEyePosition(Partial));
        int DeviceBlock = getBlockLightLevel(Device, DevicePos);
        int HolderBlock = Holder.isOnFire() ? 15 : Device.level().getBrightness(LightLayer.BLOCK, HolderPos);
        int DeviceSky = Device.level().getBrightness(LightLayer.SKY, DevicePos);
        int HolderSky = Device.level().getBrightness(LightLayer.SKY, HolderPos);
        for (int Index = 0; Index <= Segments; Index++) {
            ropePair(Consumer, Matrix, Dx, Dy, Dz, DeviceBlock, HolderBlock, DeviceSky, HolderSky, RopeWidth, SideX, SideZ, Index, Segments, false);
        }
        for (int Index = Segments; Index >= 0; Index--) {
            ropePair(Consumer, Matrix, Dx, Dy, Dz, DeviceBlock, HolderBlock, DeviceSky, HolderSky, 0.0f, SideX, SideZ, Index, Segments, true);
        }
        Pose.popPose();
    }

    private static void ropePair(VertexConsumer Consumer, Matrix4f Matrix, float Dx, float Dy, float Dz, int BlockFrom, int BlockTo, int SkyFrom, int SkyTo, float Lift,
                                 float SideX, float SideZ, int Index, int Segments, boolean Reverse) {
        float Along = (float) Index / Segments;
        int Block = (int) Mth.lerp(Along, BlockFrom, BlockTo);
        int Sky = (int) Mth.lerp(Along, SkyFrom, SkyTo);
        int Packed = LightTexture.pack(Block, Sky);
        float Shade = Index % 2 == (Reverse ? 1 : 0) ? 0.72f : 1.0f;
        float X = Dx * Along;
        float Y = Dy > 0.0f ? Dy * Along * Along : Dy - Dy * (1.0f - Along) * (1.0f - Along);
        float Z = Dz * Along;
        Consumer.addVertex(Matrix, X - SideX, Y + Lift, Z + SideZ).setColor(RopeRed * Shade, RopeGreen * Shade, RopeBlue * Shade, 1.0f).setLight(Packed);
        Consumer.addVertex(Matrix, X + SideX, Y + RopeWidth - Lift, Z - SideZ).setColor(RopeRed * Shade, RopeGreen * Shade, RopeBlue * Shade, 1.0f).setLight(Packed);
    }

    @Override
    public boolean shouldRender(Z0003Device Device, Frustum Viewing, double X, double Y, double Z) {
        return Device.tetherHolder() != null || super.shouldRender(Device, Viewing, X, Y, Z);
    }

    @Override
    public ResourceLocation getTextureLocation(Z0003Device Device) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
