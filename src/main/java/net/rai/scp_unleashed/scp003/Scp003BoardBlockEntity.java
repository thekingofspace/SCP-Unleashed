package net.rai.scp_unleashed.scp003;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Scp003BoardBlockEntity extends BlockEntity {
    public static final int MinIncubation = 1200;
    public static final int MaxIncubation = 2400;
    public static final int SyncTicks = 300;
    public static final float PulseStart = 0.4f;
    public static final float TwitchStart = 0.7f;
    public static final float CrackStart = 0.9f;
    private int Incubation;
    private int Progress;
    private long LastSync;

    public Scp003BoardBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp003Registry.BoardEntity.get(), Pos, State);
    }

    public static int stageOf(float Fraction) {
        return Fraction >= CrackStart ? 3 : Fraction >= TwitchStart ? 2 : Fraction >= PulseStart ? 1 : 0;
    }

    public float fraction() {
        return Incubation <= 0 ? 0.0f : Mth.clamp((float) Progress / Incubation, 0.0f, 1.0f);
    }

    public int getProgress() {
        return Progress;
    }

    public int getIncubation() {
        return Incubation;
    }

    public int ticksLeft() {
        return Math.max(0, Incubation - Progress);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp003BoardBlockEntity Board) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        if (Board.Incubation <= 0) {
            Board.Incubation = MinIncubation + Server.random.nextInt(MaxIncubation - MinIncubation + 1);
            Board.sync();
        }
        Board.Progress++;
        float Fraction = Board.fraction();
        int Stage = stageOf(Fraction);
        if (State.getValue(Scp003BoardBlock.Stage) != Stage) {
            World.setBlock(Pos, State.setValue(Scp003BoardBlock.Stage, Stage), Block.UPDATE_CLIENTS);
            Board.sync();
        }
        Board.cue(Server, Pos, Fraction);
        if (Board.Progress >= Board.Incubation) {
            Board.hatch(Server);
            return;
        }
        if (World.getGameTime() - Board.LastSync >= SyncTicks) {
            Board.sync();
        }
        if (Board.Progress % 20 == 0) {
            Board.setChanged();
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp003BoardBlockEntity Board) {
        if (Board.Incubation > 0 && Board.Progress < Board.Incubation) {
            Board.Progress++;
        }
        float Fraction = Board.fraction();
        RandomSource Random = World.random;
        if (Fraction >= PulseStart && Random.nextInt(Fraction >= CrackStart ? 6 : Fraction >= TwitchStart ? 14 : 30) == 0) {
            World.addParticle(Scp003Registry.Spore.get(), Pos.getX() + 0.3 + Random.nextDouble() * 0.4, Pos.getY() + 0.2, Pos.getZ() + 0.3 + Random.nextDouble() * 0.4,
                    0.0, 0.01 + Random.nextDouble() * 0.015, 0.0);
        }
    }

    private void sync() {
        if (level != null) {
            LastSync = level.getGameTime();
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void cue(ServerLevel Server, BlockPos Pos, float Fraction) {
        RandomSource Random = Server.random;
        double X = Pos.getX() + 0.5;
        double Y = Pos.getY() + 0.12;
        double Z = Pos.getZ() + 0.5;
        if (Fraction < PulseStart) {
            if (Random.nextInt(420) == 0) {
                Server.playSound(null, X, Y, Z, Scp003Registry.BoardPulse.get(), SoundSource.BLOCKS, 0.22f, 0.85f);
            }
            return;
        }
        int PulseEvery = Fraction >= CrackStart ? 26 : Fraction >= TwitchStart ? 44 : 72;
        if (Progress % PulseEvery == 0) {
            Server.playSound(null, X, Y, Z, Scp003Registry.BoardPulse.get(), SoundSource.BLOCKS, 0.3f + 0.4f * Fraction, 0.85f + 0.3f * Fraction);
        }
        if (Fraction >= TwitchStart && Random.nextInt(Fraction >= CrackStart ? 50 : 90) == 0) {
            Server.playSound(null, X, Y, Z, Scp003Registry.BoardTwitch.get(), SoundSource.BLOCKS, 0.7f, 0.9f + Random.nextFloat() * 0.2f);
            Server.sendParticles(Scp003Registry.Flesh.get(), X, Y + 0.1, Z, 3, 0.15, 0.05, 0.15, 0.04);
        }
        if (Fraction >= CrackStart && Random.nextInt(36) == 0) {
            Server.playSound(null, X, Y, Z, Scp003Registry.BoardCrack.get(), SoundSource.BLOCKS, 0.8f, 0.85f + Random.nextFloat() * 0.3f);
            Server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlockState()), X, Y + 0.15, Z, 6, 0.15, 0.05, 0.15, 0.05);
        }
    }

    public void hatchNow() {
        if (level instanceof ServerLevel Server) {
            hatch(Server);
        }
    }

    private void hatch(ServerLevel Server) {
        BlockPos Pos = worldPosition;
        BlockState State = getBlockState();
        double X = Pos.getX() + 0.5;
        double Y = Pos.getY() + 0.1;
        double Z = Pos.getZ() + 0.5;
        Server.levelEvent(2001, Pos, Block.getId(State));
        Server.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Scp003Larva Larva = Scp003Registry.Larva.get().create(Server);
        if (Larva != null) {
            float Yaw = Server.random.nextFloat() * 360.0f;
            Larva.moveTo(X, Pos.getY() + 0.02, Z, Yaw, 0.0f);
            Larva.setYHeadRot(Yaw);
            Larva.setYBodyRot(Yaw);
            Larva.beginEmerging();
            Server.addFreshEntity(Larva);
        }
        Server.playSound(null, X, Y, Z, Scp003Registry.BoardHatch.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
        Server.playSound(null, X, Y, Z, Scp003Registry.BoardHatchLow.get(), SoundSource.HOSTILE, 0.8f, 1.0f);
        Server.sendParticles(Scp003Registry.Flesh.get(), X, Y + 0.2, Z, 22, 0.25, 0.1, 0.25, 0.12);
        Server.sendParticles(Scp003Registry.Spore.get(), X, Y + 0.3, Z, 12, 0.3, 0.2, 0.3, 0.02);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Incubation", Incubation);
        Tag.putInt("Progress", Progress);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Incubation = Math.min(Tag.getInt("Incubation"), MaxIncubation);
        Progress = Tag.getInt("Progress");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("Incubation", Incubation);
        Tag.putInt("Progress", Progress);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
