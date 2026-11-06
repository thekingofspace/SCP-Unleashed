package net.rai.scp_unleashed.warhead;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AlphaWarheadBlockEntity extends BlockEntity {
    public static final int CheckTicks = 20;

    public AlphaWarheadBlockEntity(BlockPos Pos, BlockState State) {
        super(WarheadRegistry.AlphaWarheadEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, AlphaWarheadBlockEntity Warhead) {
        if (!(World instanceof ServerLevel ServerWorld) || !(State.getBlock() instanceof AlphaWarheadBlock)) {
            return;
        }
        if (Math.floorMod(World.getGameTime() + Pos.asLong(), CheckTicks) != 0) {
            return;
        }
        WarheadStatus Wanted = WarheadNetwork.get(ServerWorld).statusFor(ServerWorld, Pos);
        if (State.getValue(AlphaWarheadBlock.Status) != Wanted) {
            AlphaWarheadBlock.applyStatus(World, Pos, State, Wanted);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel ServerWorld) {
            WarheadNetwork.get(ServerWorld).addWarhead(worldPosition);
        }
    }

    public WarheadStatus getStatus() {
        BlockState State = getBlockState();
        return State.hasProperty(AlphaWarheadBlock.Status) ? State.getValue(AlphaWarheadBlock.Status) : WarheadStatus.Idle;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Status", (byte) getStatus().ordinal());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
