package net.rai.scp_unleashed.speaker;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.NetworkObjectBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.speaker.cassette.AnomalousLibrary;
import net.rai.scp_unleashed.speaker.cassette.CassetteItem;
import net.rai.scp_unleashed.speaker.client.SpeakerScreens;
import net.rai.scp_unleashed.wire.WireChannels;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SoundConsoleBlock extends NetworkObjectBlock {
    public static final MapCodec<SoundConsoleBlock> BlockCodec = simpleCodec(SoundConsoleBlock::new);
    public static final BooleanProperty Sending = BooleanProperty.create("sending");

    private final Map<Direction, VoxelShape> FacingShapes = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> SupportShapes = new EnumMap<>(Direction.class);

    public SoundConsoleBlock(Properties Properties) {
        super(Properties);
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            FacingShapes.put(Front, Shapes.or(facingBox(Front, 0.0, 0.0, 0.0, 16.0, 9.0, 16.0), facingBox(Front, 0.0, 9.0, 5.0, 16.0, 16.0, 16.0),
                    facingBox(Front, 12.0, 9.0, 0.0, 15.0, 14.0, 4.0)));
            SupportShapes.put(Front, Shapes.or(facingBox(Front, 0.0, 0.0, 0.0, 1.0, 16.0, 16.0), facingBox(Front, 15.0, 0.0, 0.0, 16.0, 16.0, 16.0),
                    facingBox(Front, 1.0, 0.0, 0.0, 15.0, 9.0, 16.0), facingBox(Front, 1.0, 9.0, 5.0, 15.0, 16.0, 16.0)));
        }
        registerDefaultState(defaultBlockState().setValue(Sending, false));
    }

    private static VoxelShape facingBox(Direction Front, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Front) {
            case SOUTH -> box(16.0 - X1, Y0, 16.0 - Z1, 16.0 - X0, Y1, 16.0 - Z0);
            case EAST -> box(16.0 - Z1, Y0, X0, 16.0 - Z0, Y1, X1);
            case WEST -> box(Z0, Y0, 16.0 - X1, Z1, Y1, 16.0 - X0);
            default -> box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected String getTooltipKey() {
        return "block.scp_unleashed.sound_console.tooltip";
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Sending);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return FacingShapes.get(State.getValue(Facing));
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return SupportShapes.get(State.getValue(Facing));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (CassetteItem.canInsert(Stack)) {
            return CassetteItem.insertInto(World, User, Hand);
        }
        return super.useItemOn(Stack, State, World, Pos, User, Hand, Hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide && World.getBlockEntity(Pos) instanceof SoundConsoleBlockEntity) {
            SpeakerScreens.openConsole(Pos);
        } else if (User instanceof ServerPlayer Listener) {
            AnomalousLibrary.sync(Listener);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbor, BlockPos NeighborPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbor, NeighborPos, MovedByPiston);
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof SoundConsoleBlockEntity Console) {
            Console.readInput();
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getTooltipKey()).withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.network_object.ports", String.format("%04d", WireChannels.MaxPort)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.sound_console.redstone").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.sound_console.loop").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.network_object.standby").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new SoundConsoleBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, SpeakerRegistry.SoundConsoleEntity.get(), SoundConsoleBlockEntity::serverTick);
    }
}
