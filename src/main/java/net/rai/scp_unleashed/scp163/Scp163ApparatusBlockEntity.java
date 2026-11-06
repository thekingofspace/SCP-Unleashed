package net.rai.scp_unleashed.scp163;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

public class Scp163ApparatusBlockEntity extends BlockEntity {
    public static final int CheckInterval = 10;
    public static final int FedWindow = 50;
    public static final int HumInterval = 100;
    public static final int DrawInterval = 60;
    public static final double Hearing = 16.0;

    private long LastFed = Long.MIN_VALUE / 2;
    private int Clock;

    public Scp163ApparatusBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp163Registry.ApparatusEntity.get(), Pos, State);
    }

    public void feed(long Now) {
        LastFed = Now;
    }

    public boolean isSupplying(long Now) {
        return Now - LastFed <= FedWindow;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel Server) {
            Scp163Breathing.get(Server).add(worldPosition);
        }
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp163ApparatusBlockEntity Machine) {
        Machine.Clock++;
        long Now = World.getGameTime();
        boolean Active = Machine.isSupplying(Now);
        if (Machine.Clock % CheckInterval == 0 && State.getValue(Scp163ApparatusBlock.Supplying) != Active) {
            World.setBlock(Pos, State.setValue(Scp163ApparatusBlock.Supplying, Active), Block.UPDATE_ALL);
        }
        if (!Active || !World.hasNearbyAlivePlayer(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Hearing)) {
            return;
        }
        if (Machine.Clock % HumInterval == 0) {
            play(World, Pos, Scp163Registry.ApparatusHum, 0.45f, 0.95f + World.random.nextFloat() * 0.1f);
        }
        if (Machine.Clock % DrawInterval == DrawInterval / 2) {
            play(World, Pos, Scp163Registry.ApparatusDraw, 0.35f, 0.9f + World.random.nextFloat() * 0.1f);
        }
    }

    private static void play(Level World, BlockPos Pos, DeferredHolder<SoundEvent, SoundEvent> Sound, float Volume, float Pitch) {
        World.playSound(null, Pos, Sound.get(), SoundSource.BLOCKS, Volume, Pitch);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Clock", Clock);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Clock = Tag.getInt("Clock");
    }
}
