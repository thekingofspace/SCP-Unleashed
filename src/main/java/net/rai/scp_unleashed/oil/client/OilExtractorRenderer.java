package net.rai.scp_unleashed.oil.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.oil.OilExtractorBlock;
import net.rai.scp_unleashed.oil.OilExtractorBlockEntity;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.List;

public class OilExtractorRenderer implements BlockEntityRenderer<OilExtractorBlockEntity> {
    public static final ModelResourceLocation BeamFrontModel = part("beam_front");
    public static final ModelResourceLocation BeamBackModel = part("beam_back");
    public static final ModelResourceLocation CrankModel = part("crank");
    public static final ModelResourceLocation PitmanModel = part("pitman");
    public static final ModelResourceLocation RodModel = part("rod");
    public static final ModelResourceLocation WireModel = part("wire");
    public static final List<ModelResourceLocation> Parts = List.of(BeamFrontModel, BeamBackModel, CrankModel, PitmanModel, RodModel, WireModel);
    public static final float PivotX = 24.0f;
    public static final float PivotY = 50.0f;
    public static final float PivotZ = 40.0f;
    public static final float[] BeamFrontOffset = {0.0f, 32.0f, 12.0f};
    public static final float[] BeamBackOffset = {14.0f, 32.0f, 52.0f};
    public static final float TailLength = 40.0f;
    public static final float HeadRadius = 32.0f;
    public static final float CrankY = 20.0f;
    public static final float CrankZ = 72.0f;
    public static final float[] CrankOffset = {10.0f, 6.0f, 56.0f};
    public static final float CrankRadius = 10.0f;
    public static final float PitmanLength = 32.0f;
    public static final float PitmanOffsetX = 10.0f;
    public static final float PitmanPinY = -16.0f;
    public static final float PitmanCentreZ = 8.0f;
    public static final float WireTop = 50.0f;
    public static final float CarrierRest = 39.0f;
    public static final float RodTop = 30.0f;
    public static final float WireLength = 16.0f;
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final RandomSource Random = RandomSource.create();

    public OilExtractorRenderer(BlockEntityRendererProvider.Context Context) {
    }

    private static ModelResourceLocation part(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/oil/extractor/" + Name));
    }

    public static float beamAngle(float Crank) {
        return (float) Math.asin(-(CrankRadius / TailLength) * Mth.sin(Crank));
    }

    @Override
    public void render(OilExtractorBlockEntity Machine, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Machine.getLevel();
        if (World == null || !(Machine.getBlockState().getBlock() instanceof OilExtractorBlock)) {
            return;
        }
        float Crank = Machine.getPhase(PartialTick) * Mth.TWO_PI;
        float Beam = beamAngle(Crank);
        float Facing = Machine.getFront().toYRot();
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Facing));
        Pose.translate(-0.5f, 0.0f, -0.5f);

        for (int Index = 0; Index < 2; Index++) {
            float[] Offset = Index == 0 ? BeamFrontOffset : BeamBackOffset;
            Pose.pushPose();
            Pose.translate(PivotX / 16.0f, PivotY / 16.0f, PivotZ / 16.0f);
            Pose.mulPose(Axis.XP.rotation(Beam));
            Pose.translate((Offset[0] - PivotX) / 16.0f, (Offset[1] - PivotY) / 16.0f, (Offset[2] - PivotZ) / 16.0f);
            draw(World, Pose, Buffers, Index == 0 ? BeamFrontModel : BeamBackModel, PackedLight, PackedOverlay);
            Pose.popPose();
        }

        Pose.pushPose();
        Pose.translate(0.0f, CrankY / 16.0f, CrankZ / 16.0f);
        Pose.mulPose(Axis.XP.rotation(Crank));
        Pose.translate(CrankOffset[0] / 16.0f, (CrankOffset[1] - CrankY) / 16.0f, (CrankOffset[2] - CrankZ) / 16.0f);
        draw(World, Pose, Buffers, CrankModel, PackedLight, PackedOverlay);
        Pose.popPose();

        float PinY = CrankY + CrankRadius * Mth.sin(Crank);
        float PinZ = CrankZ - CrankRadius * Mth.cos(Crank);
        float TailY = PivotY - TailLength * Mth.sin(Beam);
        float TailZ = PivotZ + TailLength * Mth.cos(Beam);
        float Reach = (float) Math.sqrt((TailY - PinY) * (TailY - PinY) + (TailZ - PinZ) * (TailZ - PinZ));
        float Lean = (float) Math.atan2(TailZ - PinZ, TailY - PinY);
        Pose.pushPose();
        Pose.translate(0.0f, PinY / 16.0f, PinZ / 16.0f);
        Pose.mulPose(Axis.XP.rotation(Lean));
        Pose.scale(1.0f, Reach / PitmanLength, 1.0f);
        Pose.translate(PitmanOffsetX / 16.0f, -PitmanPinY / 16.0f, -PitmanCentreZ / 16.0f);
        draw(World, Pose, Buffers, PitmanModel, PackedLight, PackedOverlay);
        Pose.popPose();

        float Carrier = CarrierRest + HeadRadius * Beam;
        Pose.pushPose();
        Pose.translate(0.0f, (Carrier - RodTop) / 16.0f, 0.0f);
        draw(World, Pose, Buffers, RodModel, PackedLight, PackedOverlay);
        Pose.popPose();

        float Span = Math.max(0.5f, WireTop - Carrier);
        Pose.pushPose();
        Pose.translate(0.0f, Carrier / 16.0f, 0.0f);
        Pose.scale(1.0f, Span / WireLength, 1.0f);
        draw(World, Pose, Buffers, WireModel, PackedLight, PackedOverlay);
        Pose.popPose();

        Pose.popPose();
    }

    private void draw(Level World, PoseStack Pose, MultiBufferSource Buffers, ModelResourceLocation Location, int PackedLight, int PackedOverlay) {
        BakedModel Model = Minecraft.getInstance().getModelManager().getModel(Location);
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.solid());
        PoseStack.Pose Last = Pose.last();
        Matrix3f Normals = Last.normal();
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                Direction Facing = Quad.getDirection();
                Vector3f Normal = Normals.transform(new Vector3f(Facing.getStepX(), Facing.getStepY(), Facing.getStepZ()));
                if (Normal.lengthSquared() > 1.0E-6f) {
                    Normal.normalize();
                }
                float Shade = World.getShade(Direction.getNearest(Normal.x(), Normal.y(), Normal.z()), Quad.isShade());
                Consumer.putBulkData(Last, Quad, Shade, Shade, Shade, 1.0f, PackedLight, PackedOverlay);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(OilExtractorBlockEntity Machine) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(OilExtractorBlockEntity Machine) {
        return OilExtractorBlock.Layout.bounds(Machine.getBlockPos(), Machine.getFront()).inflate(0.5);
    }
}
