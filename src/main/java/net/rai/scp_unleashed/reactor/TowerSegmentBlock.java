package net.rai.scp_unleashed.reactor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;
import net.rai.scp_unleashed.reactor.core.TowerType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class TowerSegmentBlock extends BaseEntityBlock {
    public static final EnumProperty<TowerSection> Section = EnumProperty.create("section", TowerSection.class);

    protected TowerSegmentBlock(Properties Properties) {
        super(Properties);
    }

    public abstract TowerType getTowerType();

    public abstract BlockState withVisual(BlockState State, int Visual);

    protected abstract int detailLines();

    protected Object[] detailArguments(int Line) {
        return new Object[0];
    }

    public String getKey() {
        return getTowerType().getName();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Section);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    public static int columnHeight(BlockGetter World, BlockPos Pos, Block Kind) {
        int Below = 0;
        while (Below < ReactorTuning.MaxHeight && World.getBlockState(Pos.below(Below + 1)).is(Kind)) {
            Below++;
        }
        int Above = 0;
        while (Above < ReactorTuning.MaxHeight && World.getBlockState(Pos.above(Above + 1)).is(Kind)) {
            Above++;
        }
        return Below + Above + 1;
    }

    public static List<BlockPos> column(BlockGetter World, BlockPos Pos, Block Kind) {
        BlockPos Low = Pos;
        int Steps = 0;
        while (Steps < ReactorTuning.MaxHeight && World.getBlockState(Low.below()).is(Kind)) {
            Low = Low.below();
            Steps++;
        }
        List<BlockPos> Found = new ArrayList<>();
        BlockPos Current = Low;
        while (Found.size() < ReactorTuning.MaxHeight && World.getBlockState(Current).is(Kind)) {
            Found.add(Current.immutable());
            Current = Current.above();
        }
        return Found;
    }

    public static List<TowerSegmentBlockEntity> segments(Level World, BlockPos Pos) {
        List<TowerSegmentBlockEntity> Found = new ArrayList<>();
        for (BlockPos Spot : column(World, Pos, World.getBlockState(Pos).getBlock())) {
            if (World.getBlockEntity(Spot) instanceof TowerSegmentBlockEntity Segment) {
                Found.add(Segment);
            }
        }
        return Found;
    }

    protected TowerSection sectionAt(BlockGetter World, BlockPos Pos) {
        return TowerSection.of(World.getBlockState(Pos.below()).is(this), World.getBlockState(Pos.above()).is(this));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        if (columnHeight(World, Pos, this) > ReactorTuning.MaxHeight) {
            Player User = Context.getPlayer();
            if (World.isClientSide && User != null) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.reactor.too_tall", ReactorTuning.MaxHeight).withStyle(ChatFormatting.GOLD), true);
            }
            return null;
        }
        return defaultBlockState().setValue(Section, sectionAt(World, Pos));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Side.getAxis() != Direction.Axis.Y) {
            return State;
        }
        boolean Below = Side == Direction.DOWN ? Neighbour.is(this) : World.getBlockState(Pos.below()).is(this);
        boolean Above = Side == Direction.UP ? Neighbour.is(this) : World.getBlockState(Pos.above()).is(this);
        return State.setValue(Section, TowerSection.of(Below, Above));
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!(World instanceof ServerLevel ServerWorld)) {
            return;
        }
        if (!OldState.is(this)) {
            ReactorGridManager.get(ServerWorld).changed(Pos);
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        } else if (OldState.getValue(Section) != State.getValue(Section)) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld) {
            ReactorGridManager.get(ServerWorld).changed(Pos);
            if (World.getBlockEntity(Pos) instanceof TowerSegmentBlockEntity Segment) {
                released(ServerWorld, Pos, Segment);
                Segment.dropContents(World, Pos);
            }
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    protected void released(ServerLevel World, BlockPos Pos, TowerSegmentBlockEntity Segment) {
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new TowerSegmentBlockEntity(Pos, State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed." + getKey() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            for (int Line = 1; Line <= detailLines(); Line++) {
                Lines.add(Component.translatable("block.scp_unleashed." + getKey() + ".detail." + Line, detailArguments(Line)).withStyle(ChatFormatting.GOLD));
            }
            Lines.add(Component.translatable("block.scp_unleashed.reactor_tower.stacking", ReactorTuning.MaxHeight).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Nullable
    protected static GridTower towerFor(Level World, BlockPos Pos) {
        return World instanceof ServerLevel ServerWorld ? ReactorGridManager.get(ServerWorld).towerAt(ServerWorld, Pos) : null;
    }

    @Nullable
    protected static ReactorGrid gridFor(Level World, BlockPos Pos) {
        return World instanceof ServerLevel ServerWorld ? ReactorGridManager.get(ServerWorld).gridAt(ServerWorld, Pos) : null;
    }
}
