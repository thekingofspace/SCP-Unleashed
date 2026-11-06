package net.rai.scp_unleashed.props;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class WaterCoolerBlock extends HorizontalDirectionalBlock implements LargeBlock {
    public static final MapCodec<WaterCoolerBlock> BlockCodec = simpleCodec(WaterCoolerBlock::new);
    private static final Map<Direction, VoxelShape> LowerOutlines = PropShapes.horizontal(PropShapes.of(box(3, 0, 3, 13, 16, 13)));
    private static final Map<Direction, VoxelShape> UpperOutlines = PropShapes.horizontal(PropShapes.of(box(3, 0, 3, 13, 7, 13), box(4, 7, 4, 12, 16, 12)));

    public WaterCoolerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TallProps.Half, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, TallProps.Half);
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        return TallProps.footprint(Context);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        TallProps.placeUpper(World, Pos, State);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        BlockState Checked = TallProps.partnerCheck(this, State, Side, NeighborState);
        return Checked != null ? Checked : super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        TallProps.creativeBreak(this, World, Pos, State, User);
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(TallProps.Half) == DoubleBlockHalf.LOWER ? LowerOutlines : UpperOutlines).get(State.getValue(FACING));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.is(Items.GLASS_BOTTLE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            ItemStack Filled = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            User.setItemInHand(Hand, ItemUtils.createFilledResult(Stack, User, Filled));
            User.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
            World.playSound(null, TallProps.lowerPos(State, Pos).above(), PropsSounds.CoolerFill.get(), SoundSource.BLOCKS, 0.8f, 0.92f + World.random.nextFloat() * 0.16f);
            World.gameEvent(User, GameEvent.FLUID_PICKUP, Pos);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(this, Lines, "fill");
    }
}
