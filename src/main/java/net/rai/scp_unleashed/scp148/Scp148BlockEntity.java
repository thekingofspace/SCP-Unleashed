package net.rai.scp_unleashed.scp148;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

public class Scp148BlockEntity extends BlockEntity {
    public static final int HumMin = 100;
    public static final int HumMax = 160;
    public static final double HumReach = 6.0;
    public static final float HumVolume = 0.5f;
    private int HumTimer;
    private Level Indexed;

    public Scp148BlockEntity(BlockPos Pos, BlockState State) {
        super(Scp148Registry.WardEntity.get(), Pos, State);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            Indexed = level;
            Scp148Index.add(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        unindex();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        unindex();
        super.onChunkUnloaded();
    }

    private void unindex() {
        if (Indexed != null) {
            Scp148Index.remove(Indexed, worldPosition);
            Indexed = null;
        }
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp148BlockEntity Ward) {
        if (--Ward.HumTimer > 0) {
            return;
        }
        Ward.HumTimer = Mth.nextInt(World.getRandom(), HumMin, HumMax);
        if (!World.hasNearbyAlivePlayer(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, HumReach)) {
            return;
        }
        float Pitch = 0.94f + World.getRandom().nextFloat() * 0.12f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp148Registry.HumLayers) {
            World.playSound(null, Pos, Layer.get(), SoundSource.BLOCKS, HumVolume, Pitch);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Lookup) {
        CompoundTag Synced = super.getUpdateTag(Lookup);
        Synced.putBoolean("Alloy", true);
        return Synced;
    }
}
