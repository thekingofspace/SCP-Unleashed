package net.rai.scp_unleashed.semanticblocker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp148.Scp148Index;
import net.rai.scp_unleashed.scp148.Scp148Shield;
import net.rai.scp_unleashed.wire.WireDevice;

import java.util.List;

public class SemanticBlockerBlockEntity extends BlockEntity implements WireDevice {
    public static final int PowerDraw = 40;
    public static final int PowerGrace = 10;
    public static final int LoadGrace = 40;
    public static final int HumMin = 120;
    public static final int HumMax = 150;
    public static final double HumReach = 12.0;
    public static final float HumVolume = 0.6f;
    public static final float SwitchVolume = 0.9f;

    private int Received;
    private int LastReceived;
    private long LastFed = Long.MIN_VALUE / 2;
    private boolean Loaded;
    private int HumTimer;
    private Level Indexed;

    public SemanticBlockerBlockEntity(BlockPos Pos, BlockState State) {
        super(SemanticBlockerRegistry.BlockerEntity.get(), Pos, State);
    }

    public static int radiusBlocks() {
        return (int) Scp148Shield.PoweredRadius;
    }

    public static void tick(Level World, BlockPos Pos, BlockState State, SemanticBlockerBlockEntity Blocker) {
        boolean Active = State.getValue(SemanticBlockerBlock.Powered);
        if (World instanceof ServerLevel Server) {
            Active = Blocker.tickServer(Server, Pos, State, Active);
        }
        Blocker.syncIndex(Active);
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
            World.setBlock(Pos, State.setValue(SemanticBlockerBlock.Powered, Active), Block.UPDATE_CLIENTS);
            play(World, Pos, Active ? SemanticBlockerRegistry.StartLayers : SemanticBlockerRegistry.StopLayers, SwitchVolume);
            HumTimer = Mth.nextInt(World.getRandom(), HumMin, HumMax);
        }
        if (Active && --HumTimer <= 0) {
            HumTimer = Mth.nextInt(World.getRandom(), HumMin, HumMax);
            if (World.hasNearbyAlivePlayer(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, HumReach)) {
                play(World, Pos, SemanticBlockerRegistry.HumLayers, HumVolume);
            }
        }
        return Active;
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
            Scp148Index.addPowered(level, worldPosition);
        } else if (!Active && Indexed != null) {
            unindex();
        }
    }

    private void unindex() {
        if (Indexed != null) {
            Scp148Index.removePowered(Indexed, worldPosition);
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
        return State.hasProperty(SemanticBlockerBlock.Powered) && State.getValue(SemanticBlockerBlock.Powered);
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

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Lookup) {
        CompoundTag Synced = super.getUpdateTag(Lookup);
        Synced.putBoolean("Telekill", true);
        return Synced;
    }
}
