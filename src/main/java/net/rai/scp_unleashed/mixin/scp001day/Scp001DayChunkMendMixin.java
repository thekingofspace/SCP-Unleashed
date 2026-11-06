package net.rai.scp_unleashed.mixin.scp001day;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.scp001day.world.Scp001DayMend;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkMap.class)
public abstract class Scp001DayChunkMendMixin {
    @Shadow
    @Final
    ServerLevel level;

    @ModifyReturnValue(method = "upgradeChunkTag(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private CompoundTag scp001dayMendHollowChunk(CompoundTag Data) {
        return Scp001DayMend.mend(level, Data);
    }
}
