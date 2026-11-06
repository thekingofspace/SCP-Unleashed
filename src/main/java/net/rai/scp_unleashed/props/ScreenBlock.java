package net.rai.scp_unleashed.props;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;
import java.util.Map;

public class ScreenBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<ScreenBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            ScreenKind.KindCodec.fieldOf("kind").forGetter(ScreenBlock::getKind), propertiesCodec()).apply(Instance, ScreenBlock::new));
    public static final int Variants = 10;
    public static final int Glow = 5;
    public static final IntegerProperty Screen = IntegerProperty.create("screen", 0, Variants - 1);
    public static final BooleanProperty Lit = BlockStateProperties.LIT;

    private final ScreenKind Kind;
    private final Map<Direction, VoxelShape> Outlines;

    public ScreenBlock(ScreenKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
        this.Outlines = PropShapes.horizontal(Kind.getShape());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Screen, Kind.getDefaultScreen()).setValue(Lit, true)
                .setValue(PropWater.Waterlogged, false));
    }

    public ScreenKind getKind() {
        return Kind;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Screen, Lit, PropWater.Waterlogged);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return PropWater.placed(defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite()), Context);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockState Next;
        SoundEvent Sound;
        if (User.isSecondaryUseActive()) {
            boolean On = !State.getValue(Lit);
            Next = State.setValue(Lit, On);
            Sound = On ? PropsSounds.ScreenOn.get() : PropsSounds.ScreenOff.get();
        } else if (!State.getValue(Lit)) {
            Next = State.setValue(Lit, true);
            Sound = PropsSounds.ScreenOn.get();
        } else {
            Next = State.setValue(Screen, (State.getValue(Screen) + 1) % Variants);
            Sound = PropsSounds.ScreenClick.get();
        }
        World.setBlock(Pos, Next, Block.UPDATE_ALL);
        World.playSound(null, Pos, Sound, SoundSource.BLOCKS, 0.6f, 0.95f + World.random.nextFloat() * 0.1f);
        World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        return InteractionResult.CONSUME;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        PropWater.update(State, World, Pos);
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return PropWater.fluid(State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.props.screen_cycle").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.props.screen_power").withStyle(ChatFormatting.GOLD));
        }
    }
}
