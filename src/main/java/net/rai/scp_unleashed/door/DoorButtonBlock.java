package net.rai.scp_unleashed.door;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DoorButtonBlock extends WallDeviceBlock implements Screwdrivable {
    public static final MapCodec<DoorButtonBlock> BlockCodec = simpleCodec(DoorButtonBlock::new);
    public static final BooleanProperty Powered = BlockStateProperties.POWERED;
    private static final Map<Direction, VoxelShape> RaisedShapes = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> PressedShapes = new EnumMap<>(Direction.class);

    static {
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            VoxelShape Base = Shapes.or(DoorGeometry.turnedBox(Front, 4, 3, 15, 12, 13, 16), DoorGeometry.turnedBox(Front, 5, 4, 14, 11, 10, 15));
            RaisedShapes.put(Front, Shapes.or(Base, DoorGeometry.turnedBox(Front, 6, 5, 12.5, 10, 9, 14)));
            PressedShapes.put(Front, Shapes.or(Base, DoorGeometry.turnedBox(Front, 6, 5, 13.5, 10, 9, 14)));
        }
    }

    public DoorButtonBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Powered, false).setValue(DevicePulse.Pulse, DevicePulse.ButtonDefault));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Powered, DevicePulse.Pulse);
    }

    @Override
    protected boolean isEmitting(BlockState State) {
        return State.getValue(Powered);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Powered) ? PressedShapes : RaisedShapes).get(State.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (State.getValue(Powered)) {
            return InteractionResult.CONSUME;
        }
        if (!World.isClientSide) {
            BlockState Pressed = State.setValue(Powered, true);
            World.setBlock(Pos, Pressed, Block.UPDATE_ALL);
            updateNeighbours(Pressed, World, Pos);
            World.scheduleTick(Pos, this, DevicePulse.ticksOf(State));
            World.playSound(null, Pos, DoorSounds.ButtonPress.get(), SoundSource.BLOCKS, 0.8f, 0.95f + World.getRandom().nextFloat() * 0.1f);
            World.gameEvent(User, GameEvent.BLOCK_ACTIVATE, Pos);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public boolean accepts(BlockState State, boolean Sneaking) {
        return Sneaking;
    }

    @Override
    public InteractionResult screwdrive(Level World, BlockPos Pos, BlockState State, @Nullable Player User, boolean Sneaking) {
        return DevicePulse.cycle(World, Pos, State, User);
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Powered)) {
            BlockState Released = State.setValue(Powered, false);
            World.setBlock(Pos, Released, Block.UPDATE_ALL);
            updateNeighbours(Released, World, Pos);
            World.playSound(null, Pos, DoorSounds.ButtonRelease.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
            World.gameEvent(null, GameEvent.BLOCK_DEACTIVATE, Pos);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.door_button.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.door_button.pulse").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.door_device.pulse_tool").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.door_device.mount").withStyle(ChatFormatting.GOLD));
        }
    }
}
