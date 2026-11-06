package net.rai.scp_unleashed.door;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
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

public class KeycardScannerBlock extends WallDeviceBlock implements Screwdrivable {
    public static final MapCodec<KeycardScannerBlock> BlockCodec = simpleCodec(KeycardScannerBlock::new);
    public static final int MaxClearance = 5;
    public static final IntegerProperty Clearance = IntegerProperty.create("level", 1, MaxClearance);
    public static final EnumProperty<ScanStatus> Status = EnumProperty.create("status", ScanStatus.class);
    public static final int DeniedTicks = 20;
    private static final Map<Direction, VoxelShape> Outlines = new EnumMap<>(Direction.class);

    static {
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            Outlines.put(Front, Shapes.or(DoorGeometry.turnedBox(Front, 4, 2, 13, 12, 14, 16), DoorGeometry.turnedBox(Front, 5, 5, 12.5, 11, 13, 13)));
        }
    }

    public KeycardScannerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Clearance, 1).setValue(Status, ScanStatus.Idle)
                .setValue(DevicePulse.Pulse, DevicePulse.ScannerDefault));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Clearance, Status, DevicePulse.Pulse);
    }

    @Override
    protected boolean isEmitting(BlockState State) {
        return State.getValue(Status) == ScanStatus.Granted;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(FACING));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!(Stack.getItem() instanceof KeycardItem Card)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (State.getValue(Status) != ScanStatus.Idle) {
            return ItemInteractionResult.CONSUME;
        }
        if (!World.isClientSide) {
            swipe(State, World, Pos, User, Card);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    private void swipe(BlockState State, Level World, BlockPos Pos, Player User, KeycardItem Card) {
        int Required = State.getValue(Clearance);
        boolean Accepted = Card.opens(Required);
        BlockState Scanned = State.setValue(Status, Accepted ? ScanStatus.Granted : ScanStatus.Denied);
        World.setBlock(Pos, Scanned, Block.UPDATE_ALL);
        if (Accepted) {
            updateNeighbours(Scanned, World, Pos);
        }
        World.scheduleTick(Pos, this, Accepted ? DevicePulse.ticksOf(State) : DeniedTicks);
        World.playSound(null, Pos, Accepted ? DoorSounds.KeycardAccept.get() : DoorSounds.KeycardDeny.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        World.gameEvent(User, GameEvent.BLOCK_ACTIVATE, Pos);
        Component Message = Accepted ? Component.translatable("message.scp_unleashed.keycard.granted").withStyle(ChatFormatting.GREEN)
                : Component.translatable("message.scp_unleashed.keycard.denied", Required).withStyle(ChatFormatting.RED);
        User.displayClientMessage(Message, true);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            int Required = State.getValue(Clearance);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.keycard.required", Required).withStyle(KeycardItem.colourOf(Required)), true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public InteractionResult screwdrive(Level World, BlockPos Pos, BlockState State, @Nullable Player User, boolean Sneaking) {
        if (Sneaking) {
            return DevicePulse.cycle(World, Pos, State, User);
        }
        int Next = State.getValue(Clearance) % MaxClearance + 1;
        World.setBlock(Pos, State.setValue(Clearance, Next), Block.UPDATE_ALL);
        World.playSound(null, Pos, DoorSounds.KeycardSet.get(), SoundSource.BLOCKS, 0.45f, 0.9f + Next * 0.08f);
        if (User != null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.keycard.set", Next).withStyle(KeycardItem.colourOf(Next)), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        ScanStatus Current = State.getValue(Status);
        if (Current != ScanStatus.Idle) {
            BlockState Reset = State.setValue(Status, ScanStatus.Idle);
            World.setBlock(Pos, Reset, Block.UPDATE_ALL);
            if (Current == ScanStatus.Granted) {
                updateNeighbours(Reset, World, Pos);
                World.gameEvent(null, GameEvent.BLOCK_DEACTIVATE, Pos);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.keycard_scanner.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.keycard_scanner.swipe").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.keycard_scanner.screwdriver").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.door_device.pulse_tool").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.door_device.mount").withStyle(ChatFormatting.GOLD));
        }
    }
}
