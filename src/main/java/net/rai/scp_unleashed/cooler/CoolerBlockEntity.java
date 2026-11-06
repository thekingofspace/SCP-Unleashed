package net.rai.scp_unleashed.cooler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.wire.WireDevice;

import java.util.List;

public class CoolerBlockEntity extends BlockEntity implements WireDevice {
    public static final int PowerDraw = 20;
    public static final int PowerGrace = 10;
    public static final int LoadGrace = 40;
    public static final double Radius = 8.0;
    public static final double HotShare = 0.5;
    public static final int HumMin = 140;
    public static final int HumMax = 180;
    public static final double HumReach = 12.0;
    public static final float HumVolume = 0.5f;
    public static final float SwitchVolume = 0.8f;

    private int Received;
    private int LastReceived;
    private long LastFed = Long.MIN_VALUE / 2;
    private boolean Loaded;
    private int HumTimer;
    private Level Indexed;
    private int Cursor;
    private int Resting;
    private int Frozen;

    public CoolerBlockEntity(BlockPos Pos, BlockState State) {
        super(CoolerRegistry.CoolerEntity.get(), Pos, State);
    }

    public static int radiusBlocks() {
        return (int) Radius;
    }

    public static double effectiveRadius(CoolerClimate Climate) {
        return Climate == CoolerClimate.Hot ? Radius * HotShare : Radius;
    }

    public static double effectiveRadius(Level World, BlockPos Pos) {
        return effectiveRadius(CoolerClimate.at(World, Pos));
    }

    public int frozen() {
        return Frozen;
    }

    public static void tick(Level World, BlockPos Pos, BlockState State, CoolerBlockEntity Cooler) {
        boolean Active = State.getValue(CoolerBlock.Powered);
        if (World instanceof ServerLevel Server) {
            Active = Cooler.tickServer(Server, Pos, State, Active);
        }
        Cooler.syncIndex(Active);
    }

    private boolean tickServer(ServerLevel World, BlockPos Pos, BlockState State, boolean WasActive) {
        long Now = World.getGameTime();
        if (!Loaded) {
            Loaded = true;
            if (WasActive) {
                LastFed = Now + LoadGrace;
            }
        }
        LastReceived = Received;
        if (Received >= PowerDraw) {
            LastFed = Math.max(LastFed, Now);
        }
        Received = 0;
        boolean Active = Now - LastFed <= PowerGrace;
        if (Active != WasActive) {
            World.setBlock(Pos, State.setValue(CoolerBlock.Powered, Active), Block.UPDATE_CLIENTS);
            play(World, Pos, Active ? CoolerRegistry.StartLayers : CoolerRegistry.StopLayers, SwitchVolume);
            HumTimer = Mth.nextInt(World.getRandom(), HumMin, HumMax);
        }
        if (Active) {
            freezeTick(World, Pos, Now);
        }
        if (Active && --HumTimer <= 0) {
            HumTimer = Mth.nextInt(World.getRandom(), HumMin, HumMax);
            if (World.hasNearbyAlivePlayer(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, HumReach)) {
                play(World, Pos, CoolerRegistry.HumLayers, HumVolume);
            }
        }
        return Active;
    }

    private void freezeTick(ServerLevel World, BlockPos Pos, long Now) {
        if (Resting > 0) {
            Resting--;
            return;
        }
        if (Math.floorMod(Now + Pos.asLong(), CoolerFreeze.Interval) != 0) {
            return;
        }
        List<BlockPos> Reach = CoolerFreeze.reach(CoolerClimate.at(World, Pos));
        for (int Step = 0; Step < CoolerFreeze.Budget; Step++) {
            if (Cursor >= Reach.size()) {
                Cursor = 0;
                Resting = CoolerFreeze.Rest;
                return;
            }
            BlockPos Target = Pos.offset(Reach.get(Cursor));
            Cursor++;
            if (World.isLoaded(Target) && CoolerFreeze.freeze(World, Target, Pos)) {
                Frozen++;
                return;
            }
        }
    }

    private static void play(ServerLevel World, BlockPos Pos, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume) {
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Pos, Layer.get(), SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    private void syncIndex(boolean Active) {
        if (Active && Indexed == null && level != null && !isRemoved()) {
            Indexed = level;
            CoolerIndex.add(level, worldPosition);
        } else if (!Active && Indexed != null) {
            unindex();
        }
    }

    private void unindex() {
        if (Indexed != null) {
            CoolerIndex.remove(Indexed, worldPosition);
            Indexed = null;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncIndex(isActive());
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

    public boolean isActive() {
        BlockState State = getBlockState();
        return State.hasProperty(CoolerBlock.Powered) && State.getValue(CoolerBlock.Powered);
    }

    public int getLastReceived() {
        return LastReceived;
    }

    @Override
    public int getPowerDemand() {
        return PowerDraw;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }
}
