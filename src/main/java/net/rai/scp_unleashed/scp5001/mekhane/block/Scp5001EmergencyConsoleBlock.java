package net.rai.scp_unleashed.scp5001.mekhane.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp5001.mekhane.Scp5001MekhaneRegistry;
import net.rai.scp_unleashed.scp5001.mekhane.client.MekhaneClientHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class Scp5001EmergencyConsoleBlock extends BaseEntityBlock {
    public static final MapCodec<Scp5001EmergencyConsoleBlock> BlockCodec = simpleCodec(Scp5001EmergencyConsoleBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty Awake = BooleanProperty.create("awake");
    private static final Map<Direction, VoxelShape> ShapeTable = new EnumMap<>(Direction.class);

    static {
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            ShapeTable.put(Side, Shapes.or(rotated(Side, 1, 0, 2, 15, 9, 14), rotated(Side, 2, 9, 7, 14, 15, 13)));
        }
    }

    public Scp5001EmergencyConsoleBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Awake, false));
    }

    public static VoxelShape rotated(Direction Side, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Side) {
            case SOUTH -> Block.box(16 - X1, Y0, 16 - Z1, 16 - X0, Y1, 16 - Z0);
            case WEST -> Block.box(Z0, Y0, 16 - X1, Z1, Y1, 16 - X0);
            case EAST -> Block.box(16 - Z1, Y0, X0, 16 - Z0, Y1, X1);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Awake);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ShapeTable.get(State.getValue(Facing));
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!State.getValue(Awake)) {
            if (!World.isClientSide) {
                World.playSound(null, Pos, Scp5001MekhaneRegistry.ConsoleDeny.get(), SoundSource.BLOCKS, 0.6f, 0.8f);
            }
            return InteractionResult.sidedSuccess(World.isClientSide);
        }
        if (World.isClientSide) {
            MekhaneClientHooks.openConsole(Pos);
        } else {
            World.playSound(null, Pos, Scp5001MekhaneRegistry.ConsoleBeep.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp5001EmergencyConsoleBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp5001MekhaneRegistry.ConsoleEntity.get(), Scp5001EmergencyConsoleBlockEntity::serverTick);
    }
}
