package net.rai.scp_unleashed.radio;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RadioMastBlock extends Block {
    public static final MapCodec<RadioMastBlock> BlockCodec = simpleCodec(RadioMastBlock::new);
    public static final BooleanProperty Top = BooleanProperty.create("top");
    public static final BooleanProperty Lit = BooleanProperty.create("lit");
    public static final int LampLight = 7;
    public static final int MaxStack = 16;
    public static final VoxelShape Column = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    public static final VoxelShape TopShape = Shapes.or(Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0), Block.box(6.5, 8.0, 6.5, 9.5, 16.0, 9.5));

    public RadioMastBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Top, true).setValue(Lit, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Top, Lit);
    }

    public static boolean holds(BlockState Below) {
        return Below.getBlock() instanceof RadioTowerBlock || Below.getBlock() instanceof RadioMastBlock;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return holds(World.getBlockState(Pos.below()));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        if (!holds(Context.getLevel().getBlockState(Pos.below()))) {
            return null;
        }
        return defaultBlockState().setValue(Top, !(Context.getLevel().getBlockState(Pos.above()).getBlock() instanceof RadioMastBlock));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Side == Direction.DOWN && !holds(Neighbour)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (Side == Direction.UP) {
            boolean Capped = !(Neighbour.getBlock() instanceof RadioMastBlock);
            BlockState Next = State.setValue(Top, Capped);
            return Capped ? Next : Next.setValue(Lit, false);
        }
        return State;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Top) ? TopShape : Column;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.radio_mast.tooltip", RadioNetwork.MaxMasts).withStyle(ChatFormatting.GRAY));
    }
}
