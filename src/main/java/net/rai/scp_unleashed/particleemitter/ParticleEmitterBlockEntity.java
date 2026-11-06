package net.rai.scp_unleashed.particleemitter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ParticleEmitterBlockEntity extends BlockEntity {
    public static final String ModeKey = "Mode";

    private static Consumer<ParticleEmitterBlockEntity> ClientTicker = Emitter -> {
    };

    private EmitterMode Mode = EmitterMode.ColdFloor;
    @Nullable
    private Object ClientState;

    public ParticleEmitterBlockEntity(BlockPos Pos, BlockState State) {
        super(ParticleEmitterRegistry.EmitterEntity.get(), Pos, State);
    }

    public static void setClientTicker(Consumer<ParticleEmitterBlockEntity> Ticker) {
        ClientTicker = Ticker;
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, ParticleEmitterBlockEntity Emitter) {
        ClientTicker.accept(Emitter);
    }

    public boolean isEmitting() {
        BlockState State = getBlockState();
        return State.hasProperty(ParticleEmitterBlock.Powered) && State.getValue(ParticleEmitterBlock.Powered);
    }

    public EmitterMode getMode() {
        return Mode;
    }

    public EmitterMode cycleMode() {
        Mode = Mode.next();
        setChanged();
        Level World = getLevel();
        if (World != null && !World.isClientSide) {
            World.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        return Mode;
    }

    public Direction getFacing() {
        BlockState State = getBlockState();
        return State.hasProperty(ParticleEmitterBlock.Facing) ? State.getValue(ParticleEmitterBlock.Facing) : Direction.UP;
    }

    @Nullable
    public Object getClientState() {
        return ClientState;
    }

    public void setClientState(@Nullable Object State) {
        ClientState = State;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putString(ModeKey, Mode.getSerializedName());
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Mode = EmitterMode.byName(Tag.getString(ModeKey));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = saveCustomOnly(Registries);
        Tag.putString(ModeKey, Mode.getSerializedName());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
