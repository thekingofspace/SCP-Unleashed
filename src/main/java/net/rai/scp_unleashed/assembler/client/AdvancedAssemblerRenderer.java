package net.rai.scp_unleashed.assembler.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.assembler.AdvancedAssemblerBlockEntity;
import net.rai.scp_unleashed.assembler.AssemblerStatus;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

import java.util.List;

public class AdvancedAssemblerRenderer implements BlockEntityRenderer<AdvancedAssemblerBlockEntity> {
    public static final ResourceLocation TurretModel = part("arm_turret");
    public static final ResourceLocation UpperModel = part("arm_upper");
    public static final ResourceLocation ForeModel = part("arm_fore");
    public static final ResourceLocation GripperModel = part("arm_gripper");
    public static final ResourceLocation FingerModel = part("arm_finger");
    public static final ResourceLocation WelderModel = part("arm_welder");
    public static final ResourceLocation PartModel = part("arm_part");
    public static final ResourceLocation TurntableModel = part("turntable");
    public static final List<ResourceLocation> Models = List.of(TurretModel, UpperModel, ForeModel, GripperModel, FingerModel, WelderModel, PartModel, TurntableModel);
    private static final ResourceLocation GlowTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/assembler/fx_glow.png");
    private static final float FingerReach = 2.2f;
    private static final float WorkScale = 0.34f;
    private static final float TurntableHeight = 2.5f;
    private static final int ChamberLight = 13;

    public AdvancedAssemblerRenderer(BlockEntityRendererProvider.Context Context) {
    }

    private static ResourceLocation part(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/assembler/" + Name);
    }

    @Override
    public void render(AdvancedAssemblerBlockEntity Assembler, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Assembler.getLevel();
        if (World == null) {
            return;
        }
        int Light = LevelRenderer.getLightColor(World, Assembler.getBlockPos().above());
        boolean Lamp = Assembler.getStatus() == AssemblerStatus.Working;
        if (Lamp) {
            Light = LightTexture.pack(Math.max(LightTexture.block(Light), ChamberLight), LightTexture.sky(Light));
        }
        float Engage = Assembler.getEngage(PartialTick);
        float Phase = Assembler.getClock(PartialTick) / AdvancedAssemblerBlockEntity.CycleTicks;
        AssemblerArms.Target Gripping = AssemblerArms.gripperTarget(Phase);
        AssemblerArms.Target Welding = AssemblerArms.welderTarget(Phase);
        AssemblerArms.Joints GripperPose = AssemblerArms.blend(AssemblerArms.Rest, AssemblerArms.solve(AssemblerArms.GripperBase, Gripping.Tip()), Engage);
        AssemblerArms.Joints WelderPose = AssemblerArms.blend(AssemblerArms.Rest, AssemblerArms.solve(AssemblerArms.WelderBase, Welding.Tip()), Engage);
        float Spread = Mth.lerp(Engage * Gripping.Grip(), AssemblerArms.FingerOpen, AssemblerArms.FingerClosed);
        boolean Engaged = Engage > 0.98f;
        float Angle = AssemblerArms.turntableAngle(Assembler.getCycles(PartialTick), Phase);
        Pose.pushPose();
        Pose.translate(0.5, 1.0, 0.5);
        Pose.mulPose(Axis.YP.rotationDegrees(-facingAngle(Assembler.getFront())));
        Pose.translate(-0.5, 0.0, -0.5);
        VertexConsumer Solid = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        arm(Pose, Solid, AssemblerArms.GripperBase, GripperPose, GripperModel, Spread, Engaged && AssemblerArms.holding(Phase), Light);
        arm(Pose, Solid, AssemblerArms.WelderBase, WelderPose, WelderModel, -1.0f, false, Light);
        Pose.pushPose();
        Pose.translate(AssemblerArms.WorkCentre.x / 16.0, TurntableHeight / 16.0, AssemblerArms.WorkCentre.z / 16.0);
        Pose.mulPose(Axis.YP.rotationDegrees(Angle));
        model(Pose, Solid, TurntableModel, Light);
        Pose.popPose();
        if (Engaged && AssemblerArms.partPlaced(Phase)) {
            Vec3 Placed = AssemblerArms.placedPart(Phase);
            Pose.pushPose();
            Pose.translate(Placed.x / 16.0, (Placed.y - FingerReach) / 16.0, Placed.z / 16.0);
            Pose.mulPose(Axis.YP.rotationDegrees(Angle));
            model(Pose, Solid, PartModel, Light);
            Pose.popPose();
        }
        workpiece(Assembler, Angle, Pose, Buffers, World, Light);
        if (Engaged && AssemblerArms.welding(Phase) && !ShaderCompat.renderingShadowPass()) {
            glow(Pose, Buffers, AssemblerArms.tip(AssemblerArms.WelderBase, WelderPose), World.getGameTime() + PartialTick);
        }
        Pose.popPose();
    }

    private static float facingAngle(Direction Front) {
        return switch (Front) {
            case EAST -> 90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };
    }

    private static void arm(PoseStack Pose, VertexConsumer Consumer, Vec3 Base, AssemblerArms.Joints Joints, ResourceLocation Tool, float Spread, boolean Holding, int Light) {
        Pose.pushPose();
        Pose.translate(Base.x / 16.0, Base.y / 16.0, Base.z / 16.0);
        Pose.mulPose(Axis.YP.rotation(Joints.Yaw()));
        model(Pose, Consumer, TurretModel, Light);
        Pose.translate(0.0, AssemblerArms.ShoulderHeight / 16.0, 0.0);
        Pose.mulPose(Axis.XP.rotation(Joints.Shoulder()));
        model(Pose, Consumer, UpperModel, Light);
        Pose.translate(0.0, AssemblerArms.UpperLength / 16.0, 0.0);
        Pose.mulPose(Axis.XP.rotation(Joints.Elbow()));
        model(Pose, Consumer, ForeModel, Light);
        Pose.translate(0.0, AssemblerArms.ForeLength / 16.0, 0.0);
        Pose.mulPose(Axis.XP.rotation(Joints.Wrist()));
        model(Pose, Consumer, Tool, Light);
        if (Spread >= 0.0f) {
            for (int Side = -1; Side <= 1; Side += 2) {
                Pose.pushPose();
                Pose.translate(0.0, 0.0, Side * Spread / 16.0);
                model(Pose, Consumer, FingerModel, Light);
                Pose.popPose();
            }
            if (Holding) {
                model(Pose, Consumer, PartModel, Light);
            }
        }
        Pose.popPose();
    }

    private static void model(PoseStack Pose, VertexConsumer Consumer, ResourceLocation Location, int Light) {
        Minecraft Game = Minecraft.getInstance();
        BakedModel Baked = Game.getModelManager().getModel(ModelResourceLocation.standalone(Location));
        ModelBlockRenderer Renderer = Game.getBlockRenderer().getModelRenderer();
        Pose.pushPose();
        Pose.translate(-0.5, -0.5, -0.5);
        Renderer.renderModel(Pose.last(), Consumer, null, Baked, 1.0f, 1.0f, 1.0f, Light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        Pose.popPose();
    }

    private static void workpiece(AdvancedAssemblerBlockEntity Assembler, float Angle, PoseStack Pose, MultiBufferSource Buffers, Level World, int Light) {
        ItemStack Display = Assembler.getDisplay();
        if (Display.isEmpty()) {
            return;
        }
        float Lift = (Display.getItem() instanceof BlockItem ? 4.0f : 8.0f) * WorkScale + 0.05f;
        Pose.pushPose();
        Pose.translate(AssemblerArms.WorkCentre.x / 16.0, (AssemblerArms.WorkCentre.y + Lift) / 16.0, AssemblerArms.WorkCentre.z / 16.0);
        Pose.mulPose(Axis.YP.rotationDegrees(Angle));
        Pose.scale(WorkScale, WorkScale, WorkScale);
        Minecraft.getInstance().getItemRenderer().renderStatic(Display, ItemDisplayContext.FIXED, Light, OverlayTexture.NO_OVERLAY, Pose, Buffers, World,
                (int) Assembler.getBlockPos().asLong());
        Pose.popPose();
    }

    private static void glow(PoseStack Pose, MultiBufferSource Buffers, Vec3 Tip, float Time) {
        float Flicker = 0.65f + 0.35f * Mth.sin(Time * 2.7f) * Mth.sin(Time * 1.3f + 0.7f);
        float Radius = (1.1f + 0.35f * Flicker) / 16.0f;
        Pose.pushPose();
        Pose.translate(Tip.x / 16.0, Tip.y / 16.0, Tip.z / 16.0);
        Pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        PoseStack.Pose Last = Pose.last();
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.eyes(GlowTexture));
        float Red = 1.0f * Flicker;
        float Green = 0.82f * Flicker;
        float Blue = 0.55f * Flicker;
        vertex(Consumer, Last, -Radius, -Radius, 0.0f, 1.0f, Red, Green, Blue);
        vertex(Consumer, Last, Radius, -Radius, 1.0f, 1.0f, Red, Green, Blue);
        vertex(Consumer, Last, Radius, Radius, 1.0f, 0.0f, Red, Green, Blue);
        vertex(Consumer, Last, -Radius, Radius, 0.0f, 0.0f, Red, Green, Blue);
        vertex(Consumer, Last, -Radius, Radius, 0.0f, 0.0f, Red, Green, Blue);
        vertex(Consumer, Last, Radius, Radius, 1.0f, 0.0f, Red, Green, Blue);
        vertex(Consumer, Last, Radius, -Radius, 1.0f, 1.0f, Red, Green, Blue);
        vertex(Consumer, Last, -Radius, -Radius, 0.0f, 1.0f, Red, Green, Blue);
        Pose.popPose();
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float U, float V, float Red, float Green, float Blue) {
        Consumer.addVertex(Last, X, Y, 0.0f)
                .setColor(Red, Green, Blue, 1.0f)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, 0.0f, 0.0f, 1.0f);
    }

    @Override
    public boolean shouldRenderOffScreen(AdvancedAssemblerBlockEntity Assembler) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(AdvancedAssemblerBlockEntity Assembler) {
        return Assembler.getRenderBounds();
    }
}
