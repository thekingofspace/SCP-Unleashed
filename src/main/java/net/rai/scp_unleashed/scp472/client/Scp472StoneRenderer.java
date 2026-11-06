package net.rai.scp_unleashed.scp472.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.scp472.Scp472BloodstoneBlock;
import net.rai.scp_unleashed.scp472.Scp472BloodstoneBlockEntity;
import net.rai.scp_unleashed.scp472.Scp472Registry;
import net.rai.scp_unleashed.scp472.Scp472Stones;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class Scp472StoneRenderer implements BlockEntityRenderer<Scp472BloodstoneBlockEntity> {
    public static final ModelResourceLocation GlowModel = ModelResourceLocation.standalone(Scp472Registry.location("block/scp472_bloodstone_glow"));
    public static final float RestGlow = 0.28f;
    public static final float BeatGlow = 0.72f;
    private static final Direction[] Sides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};

    public Scp472StoneRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float glow(BlockPos Pos, long GameTime, float Partial) {
        Scp472Stones.Nearest Heard = Scp472ClientState.heard();
        float Pulse = Heard != null ? Scp472ClientState.pulse(Partial) : Scp472ClientState.idlePulse(GameTime, Partial, Pos.asLong());
        return Math.min(1.0f, RestGlow + BeatGlow * Pulse);
    }

    @Override
    public void render(Scp472BloodstoneBlockEntity Stone, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Stone.getLevel();
        BlockState State = Stone.getBlockState();
        if (World == null || !(State.getBlock() instanceof Scp472BloodstoneBlock) || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Strength = glow(Stone.getBlockPos(), World.getGameTime(), PartialTick);
        BakedModel Model = Minecraft.getInstance().getModelManager().getModel(GlowModel);
        boolean Shaded = ShaderCompat.useFallback();
        RenderType Type = Shaded ? RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS) : RenderType.eyes(TextureAtlas.LOCATION_BLOCKS);
        float Tint = Shaded ? 1.0f : Strength;
        float Alpha = Shaded ? Strength : 1.0f;
        Pose.pushPose();
        Pose.translate(0.5f, 0.5f, 0.5f);
        turn(Pose, State.getValue(Scp472BloodstoneBlock.Facing));
        Pose.translate(-0.5f, -0.5f, -0.5f);
        VertexConsumer Consumer = Buffers.getBuffer(Type);
        RandomSource Random = RandomSource.create(42L);
        PoseStack.Pose Last = Pose.last();
        for (Direction Side : Sides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                Consumer.putBulkData(Last, Quad, Tint, Tint, Tint, Alpha, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, false);
            }
        }
        Pose.popPose();
    }

    public static void turn(PoseStack Pose, Direction Facing) {
        Pose.mulPose(Axis.YP.rotationDegrees(-quarterTurns(Facing) * 90.0f));
    }

    public static int quarterTurns(Direction Facing) {
        return switch (Facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    @Override
    public boolean shouldRenderOffScreen(Scp472BloodstoneBlockEntity Stone) {
        return false;
    }
}
