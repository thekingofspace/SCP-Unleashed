package net.rai.scp_unleashed.guards;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PatrolManagerBlock extends BaseEntityBlock {
    public static final MapCodec<PatrolManagerBlock> BlockCodec = simpleCodec(PatrolManagerBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    private static final double[][] Boxes = {{1, 0, 1, 15, 2, 15}, {3, 2, 5, 13, 9, 13}, {2, 9, 1, 14, 10, 7}, {2, 9, 7, 14, 16, 15}};
    private static final Map<Direction, VoxelShape> Outlines = new EnumMap<>(Direction.class);

    static {
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            VoxelShape Shape = Shapes.empty();
            for (double[] Part : Boxes) {
                Shape = Shapes.or(Shape, turned(Part, Side));
            }
            Outlines.put(Side, Shape.optimize());
        }
    }

    public PatrolManagerBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    private static VoxelShape turned(double[] Part, Direction Side) {
        double X0 = Part[0];
        double Y0 = Part[1];
        double Z0 = Part[2];
        double X1 = Part[3];
        double Y1 = Part[4];
        double Z1 = Part[5];
        return switch (Side) {
            case EAST -> box(16 - Z1, Y0, X0, 16 - Z0, Y1, X1);
            case SOUTH -> box(16 - X1, Y0, 16 - Z1, 16 - X0, Y1, 16 - Z0);
            case WEST -> box(Z0, Y0, 16 - X1, Z1, Y1, 16 - X0);
            default -> box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(Facing));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (!World.isClientSide && Placer instanceof Player User && World.getBlockEntity(Pos) instanceof PatrolManagerBlockEntity Manager) {
            Manager.setOwner(User);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new PatrolManagerBlockEntity(Pos, State);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (Stack.getItem() instanceof PatrolOrdersItem || Stack.getItem() instanceof PatrolLinkerItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!(World.getBlockEntity(Pos) instanceof PatrolManagerBlockEntity Manager)) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (User instanceof ServerPlayer Server) {
            Server.openMenu(new SimpleMenuProvider((Id, Pockets, Viewer) -> new PatrolManagerMenu(Id, Pockets, Manager),
                    Component.translatable("container.scp_unleashed.patrol_manager")), Buffer -> Buffer.writeBlockPos(Pos));
            GuardSounds.play(World, Vec3.atCenterOf(Pos), GuardSounds.Boot, SoundSource.BLOCKS, 0.5f, 1.0f);
        }
        return InteractionResult.CONSUME;
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
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.patrol_manager.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
