package net.rai.scp_unleashed.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GasTankBlockEntity extends BlockEntity {
    public static final int Capacity = 32000;
    public static final int NoColour = 0xFFFFFF;

    private final GasTank Tank = new GasTank(Capacity, Kind -> true, this::contentsChanged);
    private int ClientColour = NoColour;

    public GasTankBlockEntity(BlockPos Pos, BlockState State) {
        super(GasRegistry.GasTankEntity.get(), Pos, State);
    }

    public GasTank getTank() {
        return Tank;
    }

    public int getClientColour() {
        return ClientColour;
    }

    public static int fillFor(int Amount) {
        if (Amount <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(GasTankBlock.MaxFill, Math.round(Amount * (float) GasTankBlock.MaxFill / Capacity)));
    }

    private void contentsChanged() {
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState State = getBlockState();
        int Wanted = fillFor(Tank.getAmount());
        if (State.hasProperty(GasTankBlock.Fill) && State.getValue(GasTankBlock.Fill) != Wanted) {
            level.setBlock(worldPosition, State.setValue(GasTankBlock.Fill, Wanted), Block.UPDATE_CLIENTS);
        }
        int Colour = Tank.isEmpty() ? NoColour : Tank.getGas().getGas().getColour() & 0xFFFFFF;
        if (Colour != ClientColour) {
            ClientColour = Colour;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tank.save(Tag, "Gas");
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Colour")) {
            int Colour = Tag.getInt("Colour");
            if (Colour != ClientColour) {
                ClientColour = Colour;
                if (level != null && level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            return;
        }
        Tank.load(Tag, "Gas");
        ClientColour = Tank.isEmpty() ? NoColour : Tank.getGas().getGas().getColour() & 0xFFFFFF;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("Colour", Tank.isEmpty() ? NoColour : Tank.getGas().getGas().getColour() & 0xFFFFFF);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
