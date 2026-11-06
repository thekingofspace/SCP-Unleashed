package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.props.SeatBlock;
import net.rai.scp_unleashed.scp4666.Scp4666Chores;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666World;

import java.util.List;
import java.util.Map;

public class Scp4666FurnitureBlock extends HorizontalDirectionalBlock implements SeatBlock {
    public static final MapCodec<Scp4666FurnitureBlock> BlockCodec = simpleCodec(Properties -> new Scp4666FurnitureBlock(Properties, Kind.Table));

    public enum Kind {
        Counter(false, new double[][]{{0, 0, 1, 16, 14, 16}, {0, 14, 0, 16, 16, 16}}),
        Cupboard(false, new double[][]{{0, 1, 7, 16, 15, 16}}),
        Table(false, new double[][]{{0, 13, 0, 16, 16, 16}, {1, 0, 1, 3, 13, 3}, {13, 0, 1, 15, 13, 3}, {1, 0, 13, 3, 13, 15}, {13, 0, 13, 15, 13, 15}}),
        Chair(true, new double[][]{{2, 0, 2, 14, 9, 14}, {2, 9, 12, 14, 22, 14}}),
        Armchair(true, new double[][]{{1, 0, 1, 15, 9, 15}, {1, 9, 11, 15, 22, 15}, {1, 9, 1, 3, 14, 11}, {13, 9, 1, 15, 14, 11}}),
        Rocking(true, new double[][]{{2, 1, 1, 14, 9, 14}, {2, 9, 12, 14, 24, 14}}),
        Nightstand(false, new double[][]{{2, 0, 2, 14, 16, 14}}),
        Trunk(false, new double[][]{{1, 0, 3, 15, 12, 13}}),
        Washstand(false, new double[][]{{1.5, 0, 2.5, 14.5, 17, 13.5}}),
        PantryShelf(false, new double[][]{{0, 1, 9, 16, 15, 16}}),
        HangingPans(false, new double[][]{{1, 2, 13, 15, 14, 16}}),
        Boots(false, new double[][]{{3, 0, 4.5, 12, 8.5, 13.25}}),
        Picture(false, new double[][]{{2, 3, 15, 14, 14, 16}}),
        Wreath(false, new double[][]{{2.5, 0, 13, 13.5, 15, 16}}),
        RockingHorse(false, new double[][]{{4, 0, 0, 12, 16, 16}}),
        Sheeted(false, new double[][]{{0.5, 0, 0.5, 15.5, 20.5, 15.5}}),
        Icicles(false, new double[][]{{0, 8, 2.5, 16, 16, 8.5}}),
        ChoppingBlock(false, new double[][]{{2, 0, 2, 14, 9, 14}}),
        CoalBin(false, new double[][]{{1, 0, 1, 15, 11, 15}}),
        Workbench(false, new double[][]{{0, 0, 1, 16, 15, 15}}),
        MoonBoard(false, new double[][]{{0, 0, 0, 16, 16, 3}}),
        Parcel(false, new double[][]{{4, 0, 4, 12, 7, 12}});

        public final boolean Seat;
        public final Map<Direction, VoxelShape> Shapes;

        Kind(boolean Seat, double[][] Boxes) {
            this.Seat = Seat;
            this.Shapes = Scp4666Shapes.table(Boxes);
        }
    }

    private final Kind Type;

    public Scp4666FurnitureBlock(Properties Properties, Kind Type) {
        super(Properties);
        this.Type = Type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return Type;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Type.Shapes.get(State.getValue(FACING));
    }

    @Override
    public boolean seats(BlockState State) {
        return Type.Seat;
    }

    public boolean chore() {
        return Type == Kind.ChoppingBlock || Type == Kind.CoalBin || Type == Kind.Workbench || Type == Kind.Trunk || Type == Kind.PantryShelf;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (chore() && Scp4666World.inside(World)) {
            if (World instanceof ServerLevel Server && User instanceof ServerPlayer Visitor) {
                Scp4666Chores.use(Server, Visitor, Pos, Type);
            }
            return InteractionResult.CONSUME;
        }
        if (!Type.Seat || User.isSecondaryUseActive() || User.isPassenger() || PropSeatEntity.occupied(World, Pos)) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (PropSeatEntity.sit(World, Pos, State, User)) {
            if (Type == Kind.Rocking) {
                World.playSound(null, Pos, Scp4666Registry.Creak.get(), SoundSource.BLOCKS, 0.5f, 1.3f);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (Type.Seat && !World.isClientSide && !State.is(NewState.getBlock())) {
            PropSeatEntity.release(World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_furniture.tooltip").withStyle(ChatFormatting.GRAY));
        if (Type.Seat) {
            Lines.add(Component.translatable("block.scp_unleashed.scp4666_furniture.sit").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
