package net.rai.scp_unleashed.scp049;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp049TrayBlock extends BaseEntityBlock {
    public static final MapCodec<Scp049TrayBlock> BlockCodec = simpleCodec(Scp049TrayBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape Stand = Shapes.or(Block.box(4, 0, 4, 12, 1, 12), Block.box(7, 1, 7, 9, 10, 9));
    private static final VoxelShape AlongX = Shapes.or(Stand, Block.box(2, 10, 3.5, 14, 14, 12.5));
    private static final VoxelShape AlongZ = Shapes.or(Stand, Block.box(3.5, 10, 2, 12.5, 14, 14));

    public Scp049TrayBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
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
        return State.getValue(Facing).getAxis() == Direction.Axis.Z ? AlongX : AlongZ;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (!(World instanceof ServerLevel Server) || Placer == null) {
            return;
        }
        Vec3 Centre = Vec3.atCenterOf(Pos);
        Scp049Doctor Best = null;
        double BestDistance = Scp049Doctor.LinkRange * Scp049Doctor.LinkRange;
        for (Scp049Doctor Doctor : Server.getEntitiesOfClass(Scp049Doctor.class, new AABB(Centre, Centre).inflate(Scp049Doctor.LinkRange), Scp049Doctor::isAlive)) {
            double Distance = Doctor.distanceToSqr(Centre);
            if (Distance < BestDistance && Doctor.needsTray(Server)) {
                BestDistance = Distance;
                Best = Doctor;
            }
        }
        if (Best != null) {
            Best.linkTray(Server, Pos, true);
        }
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp049_surgery_tray.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp049_surgery_tray.home").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp049_surgery_tray.move").withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable("block.scp_unleashed.scp049_surgery_tray.free").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp049TrayBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp049Registry.TrayEntity.get(), Scp049TrayBlockEntity::serverTick);
    }
}
