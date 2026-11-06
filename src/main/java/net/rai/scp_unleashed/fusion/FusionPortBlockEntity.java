package net.rai.scp_unleashed.fusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

public class FusionPortBlockEntity extends BlockEntity implements WireDevice {
    public FusionPortBlockEntity(BlockPos Pos, BlockState State) {
        super(FusionRegistry.PortEntity.get(), Pos, State);
    }

    public boolean isInput() {
        BlockState State = getBlockState();
        return State.getBlock() instanceof FusionReactorBlock && State.getValue(FusionReactorBlock.Part) == FusionLayout.InputPart;
    }

    @Nullable
    public FusionReactorBlockEntity reactor() {
        BlockState State = getBlockState();
        if (level == null || !(State.getBlock() instanceof FusionReactorBlock)) {
            return null;
        }
        BlockPos MasterPos = FusionLayout.masterOf(worldPosition, State);
        return level.isLoaded(MasterPos) && level.getBlockEntity(MasterPos) instanceof FusionReactorBlockEntity Reactor ? Reactor : null;
    }

    @Override
    public boolean isGenerator() {
        return !isInput();
    }

    @Override
    public int getPowerOutput() {
        if (isInput()) {
            return 0;
        }
        FusionReactorBlockEntity Reactor = reactor();
        return Reactor == null ? 0 : Reactor.offerOutput();
    }

    @Override
    public void extractPower(int Amount) {
        FusionReactorBlockEntity Reactor = isInput() ? null : reactor();
        if (Reactor != null) {
            Reactor.drawOutput(Amount);
        }
    }

    @Override
    public int getGenerationRate() {
        FusionReactorBlockEntity Reactor = isInput() ? null : reactor();
        return Reactor == null ? 0 : Reactor.getGenerationRate();
    }

    @Override
    public int getPowerDemand() {
        FusionReactorBlockEntity Reactor = isInput() ? reactor() : null;
        return Reactor == null ? 0 : Reactor.chargeDemand();
    }

    @Override
    public void insertPower(int Amount) {
        FusionReactorBlockEntity Reactor = isInput() ? reactor() : null;
        if (Reactor != null) {
            Reactor.receiveCharge(Amount);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Input", isInput());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
