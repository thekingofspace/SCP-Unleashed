package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.entity.RedstoneSenderBlockEntity;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import org.jetbrains.annotations.Nullable;

public class RedstoneSenderBlock extends RedstonePortBlock {
    public static final MapCodec<RedstoneSenderBlock> BlockCodec = simpleCodec(RedstoneSenderBlock::new);

    public RedstoneSenderBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected String getTooltipKey() {
        return "block.scp_unleashed.redstone_sender.tooltip";
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbor, BlockPos NeighborPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbor, NeighborPos, MovedByPiston);
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof RedstoneSenderBlockEntity Sender) {
            Sender.readInput();
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new RedstoneSenderBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, ModBlockEntityTypes.RedstoneSender.get(), RedstoneSenderBlockEntity::serverTick);
    }
}
