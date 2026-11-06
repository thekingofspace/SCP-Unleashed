package net.rai.scp_unleashed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.block.MiniTankBlock;
import net.rai.scp_unleashed.block.entity.MiniTankBlockEntity;

public class MiniTankRenderer implements BlockEntityRenderer<MiniTankBlockEntity> {
    private static final float Wall = 1.5f / 16.0f;
    private static final float Cap = 2.0f / 16.0f;

    public MiniTankRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(MiniTankBlockEntity Tank, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        FluidStack Liquid = Tank.getShownLiquid();
        float Fill = Math.min(1.0f, Tank.getShownFill(PartialTick));
        if (Liquid.isEmpty() || Fill <= 0.0f) {
            return;
        }
        BlockState State = Tank.getBlockState();
        boolean TopExposed = !State.hasProperty(MiniTankBlock.Top) || State.getValue(MiniTankBlock.Top);
        boolean BottomExposed = !State.hasProperty(MiniTankBlock.Bottom) || State.getValue(MiniTankBlock.Bottom);
        float Low = BottomExposed ? Cap : 0.0f;
        float High = TopExposed ? 1.0f - Cap : 1.0f;
        float Surface = Low + (High - Low) * Fill;
        boolean DrawTop = Fill < 0.999f || TopExposed || !fluidAbove(Tank, PartialTick);
        FluidRendering.renderBox(Pose, Buffers, Liquid, Tank.getLevel(), Tank.getBlockPos(), Wall, Low, Wall, 1.0f - Wall, Surface, 1.0f - Wall, PackedLight, DrawTop, BottomExposed);
    }

    private static boolean fluidAbove(MiniTankBlockEntity Tank, float PartialTick) {
        Level World = Tank.getLevel();
        return World != null && World.getBlockEntity(Tank.getBlockPos().above()) instanceof MiniTankBlockEntity Above
                && !Above.getShownLiquid().isEmpty() && Above.getShownFill(PartialTick) > 0.0f;
    }
}
