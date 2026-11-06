package net.rai.scp_unleashed.mixin;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.rai.scp_unleashed.fluiddump.client.PollutionClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColors.class)
public abstract class WaterTintMixin {
    @Inject(method = "getAverageWaterColor", at = @At("RETURN"), cancellable = true)
    private static void pollutedWaterTint(BlockAndTintGetter World, BlockPos Pos, CallbackInfoReturnable<Integer> Info) {
        int Base = Info.getReturnValueI();
        int Tinted = PollutionClient.tint(Base, Pos);
        if (Tinted != Base) {
            Info.setReturnValue(Tinted);
        }
    }
}
