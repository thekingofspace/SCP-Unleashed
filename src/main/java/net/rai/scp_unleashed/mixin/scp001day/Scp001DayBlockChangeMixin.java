package net.rai.scp_unleashed.mixin.scp001day;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp001day.client.Scp001DaySunGrid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class Scp001DayBlockChangeMixin {
    @Inject(method = "blockChanged", at = @At("HEAD"))
    private void scp001dayBlockChanged(BlockGetter Level, BlockPos Pos, BlockState Old, BlockState New, int Flags, CallbackInfo Info) {
        if (Scp001DaySunGrid.ready()) {
            Scp001DaySunGrid.markDirty(Pos);
        }
    }
}
