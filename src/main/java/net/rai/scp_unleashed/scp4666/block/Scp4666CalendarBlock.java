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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp4666.Scp4666Map;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666State;
import net.rai.scp_unleashed.scp4666.Scp4666World;

import java.util.List;
import java.util.Map;

public class Scp4666CalendarBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp4666CalendarBlock> BlockCodec = simpleCodec(Scp4666CalendarBlock::new);
    public static final IntegerProperty Opened = IntegerProperty.create("opened", 0, Scp4666State.FinalNight);
    private static final Map<Direction, VoxelShape> Shapes = Scp4666Shapes.table(new double[][]{{2, 1, 15, 14, 15, 16}});

    public Scp4666CalendarBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Opened, 1));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Opened);
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

    public static void mark(ServerLevel World, Scp4666Map Layout, int Night) {
        BlockState Found = World.getBlockState(Layout.Calendar);
        if (Found.getBlock() instanceof Scp4666CalendarBlock) {
            World.setBlock(Layout.Calendar, Found.setValue(Opened, Math.max(0, Math.min(Scp4666State.FinalNight, Night))), Block.UPDATE_ALL);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        World.playSound(null, Pos, Scp4666Registry.Page.get(), SoundSource.BLOCKS, 0.7f, 1.0f);
        if (World instanceof ServerLevel Server && Scp4666World.inside(World)) {
            int Night = Scp4666State.get(Server.getServer()).night();
            mark(Server, Scp4666Map.get(Server.getServer()), Night);
            Scp4666State Data = Scp4666State.get(Server.getServer());
            String Daily = "calendar:" + User.getUUID();
            if (!Data.daily(Daily) && Data.started() && !User.isSpectator()) {
                Data.markDaily(Daily);
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.calendar_hint." + Math.max(1, Math.min(Scp4666State.FinalNight, Night)))
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                return InteractionResult.CONSUME;
            }
            String Key = Night >= Scp4666State.FinalNight ? "calendar_last" : "calendar";
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666." + Key, Night, Scp4666State.FinalNight).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        int Next = State.getValue(Opened) >= Scp4666State.FinalNight ? 0 : State.getValue(Opened) + 1;
        World.setBlock(Pos, State.setValue(Opened, Next), Block.UPDATE_ALL);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_advent_calendar.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
