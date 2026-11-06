package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp4666.Scp4666Director;
import net.rai.scp_unleashed.scp4666.Scp4666Map;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666State;
import net.rai.scp_unleashed.scp4666.Scp4666World;

import java.util.List;
import java.util.Map;

public class Scp4666SwitchBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp4666SwitchBlock> BlockCodec = simpleCodec(Scp4666SwitchBlock::new);
    public static final BooleanProperty On = BlockStateProperties.POWERED;
    public static final int Reach = 12;
    private static final Map<Direction, VoxelShape> Shapes = Scp4666Shapes.table(new double[][]{{5, 3, 14, 11, 13, 16}});

    public Scp4666SwitchBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(On, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, On);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Face = Context.getClickedFace();
        if (Face.getAxis().isVertical()) {
            Face = Context.getHorizontalDirection().getOpposite();
        }
        BlockState State = defaultBlockState().setValue(FACING, Face);
        return State.canSurvive(Context.getLevel(), Context.getClickedPos()) ? State : null;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        BlockPos Wall = Pos.relative(State.getValue(FACING).getOpposite());
        return World.getBlockState(Wall).isFaceSturdy(World, Wall, State.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Other, LevelAccessor World, BlockPos Pos, BlockPos OtherPos) {
        return Side == State.getValue(FACING).getOpposite() && !State.canSurvive(World, Pos) ? Blocks.AIR.defaultBlockState() : State;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean Next = !State.getValue(On);
        World.setBlock(Pos, State.setValue(On, Next), Block.UPDATE_ALL);
        World.playSound(null, Pos, Scp4666Registry.SwitchClick.get(), SoundSource.BLOCKS, 0.6f, Next ? 1.1f : 0.9f);
        if (World instanceof ServerLevel Server && Scp4666World.inside(World)) {
            Scp4666State Data = Scp4666State.get(Server.getServer());
            Data.setLights(Next);
            Scp4666Director.refreshLamps(Server, Scp4666Map.get(Server.getServer()), Data);
            if (Data.lightsDead()) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.switch_dead").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
            return InteractionResult.CONSUME;
        }
        Scp4666LampBlock.switchAround(World, Pos, Next, Reach);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_light_switch.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
