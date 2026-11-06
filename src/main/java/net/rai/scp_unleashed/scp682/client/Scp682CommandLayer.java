package net.rai.scp_unleashed.scp682.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp682.Scp682;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class Scp682CommandLayer extends BlockAndItemGeoLayer<Scp682> {
    public static final String CommandBone = "command";
    public static final float BlockSize = 0.32f;
    public static final float Sunk = 0.4f;

    public Scp682CommandLayer(GeoRenderer<Scp682> Renderer) {
        super(Renderer);
    }

    @Nullable
    @Override
    protected BlockState getBlockForBone(GeoBone Bone, Scp682 Owner) {
        if (!CommandBone.equals(Bone.getName()) || Owner.commandShown(Owner.renderPartial()) <= 0.001f) {
            return null;
        }
        return Blocks.COMMAND_BLOCK.defaultBlockState();
    }

    @Override
    protected void renderBlockForBone(PoseStack Pose, GeoBone Bone, BlockState State, Scp682 Owner, MultiBufferSource Buffers, float Partial, int Light, int Overlay) {
        float Growth = Owner.commandShown(Partial);
        float Side = BlockSize * Growth;
        Pose.pushPose();
        Pose.translate(-Side * 0.5f, -Side * Sunk, -Side * 0.5f);
        Pose.scale(Side, Side, Side);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(State, Pose, Buffers, Light, OverlayTexture.NO_OVERLAY);
        Pose.popPose();
    }
}
