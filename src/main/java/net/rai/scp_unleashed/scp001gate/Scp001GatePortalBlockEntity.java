package net.rai.scp_unleashed.scp001gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Scp001GatePortalBlockEntity extends BlockEntity {
    private BlockPos Centre = BlockPos.ZERO;
    private Direction Front = Direction.SOUTH;
    private boolean Home;
    private boolean Bound;

    public Scp001GatePortalBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp001GateRegistry.PortalEntity.get(), Pos, State);
    }

    public void bind(BlockPos Gate, Direction Facing) {
        Centre = Gate.immutable();
        Front = Facing;
        Bound = true;
        changed();
    }

    public void home(boolean Back) {
        Home = Back;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public BlockPos centre() {
        return Bound ? Centre : worldPosition;
    }

    public Direction front() {
        return Front;
    }

    public boolean leadsHome() {
        return Home;
    }

    public boolean bound() {
        return Bound;
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        write(Saved);
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        if (Saved.contains("Centre")) {
            Centre = NbtUtils.readBlockPos(Saved, "Centre").orElse(worldPosition);
            Direction Facing = Direction.byName(Saved.getString("Front"));
            Front = Facing != null && Facing.getAxis().isHorizontal() ? Facing : Direction.SOUTH;
            Home = Saved.getBoolean("Home");
            Bound = true;
        }
    }

    private void write(CompoundTag Saved) {
        if (!Bound) {
            return;
        }
        Saved.put("Centre", NbtUtils.writeBlockPos(Centre));
        Saved.putString("Front", Front.getSerializedName());
        Saved.putBoolean("Home", Home);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Saved = new CompoundTag();
        write(Saved);
        return Saved;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
