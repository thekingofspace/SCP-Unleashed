package net.rai.scp_unleashed.daevite.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class DaeviteDecorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DaeviteDecorBlock> DecorCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            propertiesCodec(),
            DaeviteDecor.KindCodec.fieldOf("kind").forGetter(DaeviteDecorBlock::kind)
    ).apply(Instance, DaeviteDecorBlock::new));

    private final DaeviteDecor Kind;

    public DaeviteDecorBlock(Properties Settings, DaeviteDecor Kind) {
        super(Settings);
        this.Kind = Kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public DaeviteDecor kind() {
        return Kind;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return DecorCodec;
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
        return Kind.outline(State.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Kind.solid(State.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(FACING, Turn.rotate(State.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(FACING)));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Key = getDescriptionId();
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.daevite_decor.details").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    public void ambient(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        ambient(State, World, Pos, Random);
    }
}
