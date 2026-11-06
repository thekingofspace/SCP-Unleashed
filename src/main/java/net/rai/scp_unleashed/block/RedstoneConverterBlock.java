package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.entity.RedstoneConverterBlockEntity;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class RedstoneConverterBlock extends RedstonePortBlock {
    public static final MapCodec<RedstoneConverterBlock> BlockCodec = simpleCodec(RedstoneConverterBlock::new);
    private static final Map<Direction, VoxelShape> ShapeByFacing = buildShapes();

    public RedstoneConverterBlock(Properties Properties) {
        super(Properties);
    }

    private static Map<Direction, VoxelShape> buildShapes() {
        Map<Direction, VoxelShape> Built = new EnumMap<>(Direction.class);
        VoxelShape Shell = Shapes.or(Block.box(0, 0, 0, 16, 2, 16), Block.box(0, 9, 0, 16, 16, 16));
        Built.put(Direction.NORTH, Shapes.or(Shell, Block.box(0, 2, 14, 16, 9, 16)));
        Built.put(Direction.SOUTH, Shapes.or(Shell, Block.box(0, 2, 0, 16, 9, 2)));
        Built.put(Direction.WEST, Shapes.or(Shell, Block.box(14, 2, 0, 16, 9, 16)));
        Built.put(Direction.EAST, Shapes.or(Shell, Block.box(0, 2, 0, 2, 9, 16)));
        return Built;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected String getTooltipKey() {
        return "block.scp_unleashed.redstone_converter.tooltip";
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ShapeByFacing.get(State.getValue(Facing));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new RedstoneConverterBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, ModBlockEntityTypes.RedstoneConverter.get(), RedstoneConverterBlockEntity::serverTick);
    }
}
