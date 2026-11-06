package net.rai.scp_unleashed.scp1730.rooms;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Scp1730TelevisionBlock extends BaseEntityBlock {
    public static final MapCodec<Scp1730TelevisionBlock> TelevisionCodec = simpleCodec(Scp1730TelevisionBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Screen> ScreenState = EnumProperty.create("screen", Screen.class);
    private static final VoxelShape NorthSouth = Shapes.or(Block.box(1, 0, 2, 15, 12, 14), Block.box(4, 12, 6, 12, 13, 10));
    private static final VoxelShape EastWest = Shapes.or(Block.box(2, 0, 1, 14, 12, 15), Block.box(6, 12, 4, 10, 13, 12));

    public enum Screen implements StringRepresentable {
        Off("off", 0),
        Noise("static", 6),
        Show("show", 10);

        private final String Name;
        private final int Glow;

        Screen(String Name, int Glow) {
            this.Name = Name;
            this.Glow = Glow;
        }

        public int glow() {
            return Glow;
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    public Scp1730TelevisionBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(ScreenState, Screen.Off));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return TelevisionCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, ScreenState);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Facing).getAxis() == Direction.Axis.Z ? NorthSouth : EastWest;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp1730TelevisionBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, Scp1730RoomsRegistry.TelevisionEntity.get(), World.isClientSide ? Scp1730TelevisionBlockEntity::clientTick : Scp1730TelevisionBlockEntity::serverTick);
    }
}
