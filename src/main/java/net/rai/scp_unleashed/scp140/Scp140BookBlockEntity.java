package net.rai.scp_unleashed.scp140;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public class Scp140BookBlockEntity extends BlockEntity {
    public static final float OpenVolume = 0.55f;
    public static final float TurnVolume = 0.35f;
    public static final int MoteChance = 22;
    public static final double PageHeight = 0.16;

    private final Scp140Motion Motion = new Scp140Motion();
    private int Written;
    private int Mood;

    public Scp140BookBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp140Registry.BookEntity.get(), Pos, State);
    }

    public Scp140Motion getMotion() {
        return Motion;
    }

    public int getWritten() {
        return Written;
    }

    public Scp140Mood getMood() {
        return Scp140Mood.byId(Mood);
    }

    public boolean isInked() {
        return Written > 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel World) {
            Scp140Witnesses.placed(World, worldPosition, true);
            refresh();
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel World) {
            Scp140Witnesses.placed(World, worldPosition, false);
        }
        super.setRemoved();
    }

    public void refresh() {
        if (!(level instanceof ServerLevel World)) {
            return;
        }
        Scp140Chronicle Chronicle = Scp140Chronicle.get(World.getServer());
        int NowWritten = Chronicle.written();
        int NowMood = Chronicle.mood(World.getGameTime()).ordinal();
        if (NowWritten != Written || NowMood != Mood) {
            Written = NowWritten;
            Mood = NowMood;
            setChanged();
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp140BookBlockEntity Book) {
        RandomSource Random = World.getRandom();
        Vec3 Centre = Vec3.atBottomCenterOf(Pos).add(0.0, PageHeight, 0.0);
        Scp140Motion.Event Happened = Book.Motion.step(World, Centre, true, Random);
        switch (Happened) {
            case Opened -> play(World, Centre, Scp140Registry.OpenLayers, OpenVolume, 0.95f + Random.nextFloat() * 0.1f);
            case Closed -> play(World, Centre, Scp140Registry.CloseLayers, OpenVolume, 0.95f + Random.nextFloat() * 0.1f);
            case Turned -> play(World, Centre, Scp140Registry.TurnLayers, TurnVolume, 0.9f + Random.nextFloat() * 0.2f);
            default -> {
            }
        }
        if (Book.Motion.open(1.0f) > 0.8f && Book.getMood() != Scp140Mood.Dry && Random.nextInt(MoteChance) == 0) {
            World.addParticle(Scp140Registry.Ink.get(), Centre.x + (Random.nextDouble() - 0.5) * 0.6, Centre.y + 0.05, Centre.z + (Random.nextDouble() - 0.5) * 0.5,
                    0.0, 0.012 + Random.nextDouble() * 0.01, 0.0);
        }
    }

    private static void play(Level World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playLocalSound(Where.x, Where.y, Where.z, Layer.get(), SoundSource.BLOCKS, Volume, Pitch, false);
        }
    }

    public AABB getRenderBounds() {
        return new AABB(worldPosition).inflate(0.5, 0.0, 0.5).expandTowards(0.0, 0.5, 0.0);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Written")) {
            Written = Tag.getInt("Written");
        }
        if (Tag.contains("Mood")) {
            Mood = Tag.getInt("Mood");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("Written", Written);
        Tag.putInt("Mood", Mood);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
