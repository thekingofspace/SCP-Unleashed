package net.rai.scp_unleashed.labware.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.labware.LabViralStorageBlock;
import net.rai.scp_unleashed.labware.LabViralStorageBlockEntity;
import net.rai.scp_unleashed.labware.LabwareRegistry;
import net.rai.scp_unleashed.labware.LabwareSounds;
import net.rai.scp_unleashed.storage.StorageRegistry;

public final class LabwareEffects {
    public static final int VentSoundTicks = 60;
    public static final int PlumesPerTick = 3;
    public static final int MistChance = 2;
    public static final int ChillChance = 160;
    public static final double HatchSpread = 0.34;
    public static final double MistSpread = 0.28;
    public static final int Untinted = 0xFFFFFF;

    private LabwareEffects() {
    }

    public static void tickStorage(LabViralStorageBlockEntity Locker) {
        Level World = Locker.getLevel();
        if (World == null || !World.isClientSide) {
            return;
        }
        BlockState State = Locker.getBlockState();
        if (!(State.getBlock() instanceof LabViralStorageBlock) || !State.getValue(LabViralStorageBlock.Hatch)) {
            return;
        }
        BlockPos Pos = Locker.getBlockPos();
        RandomSource Random = World.getRandom();
        double Top = Pos.getY() + LabViralStorageBlockEntity.HatchHeight;
        if (State.getValue(LabViralStorageBlock.Venting)) {
            int[] Mist = Locker.getMistColours();
            for (int Count = 0; Count < PlumesPerTick; Count++) {
                double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * HatchSpread;
                double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * HatchSpread;
                int Colour = Mist.length == 0 ? Untinted : Mist[Random.nextInt(Mist.length)];
                World.addParticle(ColorParticleOption.create(LabwareRegistry.VentPlume.get(), Colour | 0xFF000000), X, Top, Z,
                        (Random.nextDouble() - 0.5) * 0.006, 0.015, (Random.nextDouble() - 0.5) * 0.006);
            }
            if (Math.floorMod(World.getGameTime() + Pos.asLong(), VentSoundTicks) == 0) {
                float Pitch = 0.95f + Random.nextFloat() * 0.1f;
                World.playLocalSound(Pos, LabwareSounds.StorageVent.get(), SoundSource.BLOCKS, 1.0f, Pitch, false);
                World.playLocalSound(Pos, LabwareSounds.StorageVentHiss.get(), SoundSource.BLOCKS, 1.0f, Pitch, false);
            }
            return;
        }
        if (!State.getValue(LabViralStorageBlock.Chilled)) {
            return;
        }
        if (Random.nextInt(MistChance) == 0) {
            double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * MistSpread;
            double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * MistSpread;
            World.addParticle(StorageRegistry.Mist.get(), X, Top, Z, (Random.nextDouble() - 0.5) * 0.01, 0.004, (Random.nextDouble() - 0.5) * 0.01);
        }
        if (Random.nextInt(ChillChance) == 0) {
            World.playLocalSound(Pos, LabwareSounds.StorageChill.get(), SoundSource.BLOCKS, 1.0f, 0.95f + Random.nextFloat() * 0.1f, false);
        }
    }
}
