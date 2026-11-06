package net.rai.scp_unleashed.scp179;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.archive.UniqueGifts;
import net.rai.scp_unleashed.computer.DeviceShapes;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp179.client.Scp179ClientHooks;
import net.rai.scp_unleashed.wire.OutletMounts;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SatelliteTerminalBlock extends BaseEntityBlock {
    public static final MapCodec<SatelliteTerminalBlock> BlockCodec = simpleCodec(SatelliteTerminalBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<TerminalDisplay> Display = EnumProperty.create("screen", TerminalDisplay.class);
    public static final double[][] Boxes = {{0, 0, 0, 16, 9, 16}, {2, 9, 1, 14, 10, 7}, {1, 9, 8, 15, 16, 16}};
    public static final double[][] Supports = supportBoxes();
    public static final float LinkVolume = 0.6f;

    private final Map<Direction, VoxelShape> Outline = DeviceShapes.turned(Boxes);
    private final Map<Direction, VoxelShape> Support = DeviceShapes.turned(Supports);

    public SatelliteTerminalBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Display, TerminalDisplay.Off));
    }

    private static double[][] supportBoxes() {
        List<AABB> Pixels = new ArrayList<>();
        for (double[] Box : Boxes) {
            Pixels.add(new AABB(Box[0], Box[1], Box[2], Box[3], Box[4], Box[5]));
        }
        List<double[]> Plates = new ArrayList<>();
        for (Direction Side : OutletMounts.faces(Pixels, 1, 1, 1, 0, 0, 0)) {
            AABB Plate = OutletMounts.plate(Side);
            Plates.add(new double[]{Plate.minX, Plate.minY, Plate.minZ, Plate.maxX, Plate.maxY, Plate.maxZ});
        }
        return Plates.toArray(double[][]::new);
    }

    public static int lightOf(BlockState State) {
        return State.hasProperty(Display) ? State.getValue(Display).getLight() : 0;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Display);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outline.get(State.getValue(Facing));
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Support.get(State.getValue(Facing));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.is(Scp179Registry.Coordinator.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof SatelliteTerminalBlockEntity Terminal && register(Terminal, User, Stack)
                && !UniqueGifts.keeps(User)) {
            Stack.consume(1, User);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    public static boolean register(SatelliteTerminalBlockEntity Terminal, Player User, ItemStack Stack) {
        ResourceLocation Target = SpaceCoordinatorItem.target(Stack);
        if (Target == null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.satellite_terminal.blank").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        Component Label = SkyTargets.label(Target);
        SatelliteTerminalBlockEntity.AddResult Result = Terminal.addLocation(Target);
        if (Result == SatelliteTerminalBlockEntity.AddResult.Full) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.satellite_terminal.full", SatelliteTerminalBlockEntity.MaxLocations)
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (User instanceof ServerPlayer Viewer) {
            SkyWatch.get(Viewer.server).add(Target, Viewer.getUUID());
        }
        if (Result == SatelliteTerminalBlockEntity.AddResult.Known) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.satellite_terminal.known", Label).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        User.displayClientMessage(Component.translatable("message.scp_unleashed.satellite_terminal.added", Label).withStyle(ChatFormatting.AQUA), true);
        if (Terminal.getLevel() instanceof ServerLevel World) {
            BlockPos Pos = Terminal.getBlockPos();
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp179Registry.LinkLayers) {
                World.playSound(null, Pos, Layer.get(), SoundSource.BLOCKS, LinkVolume, 1.0f);
            }
        }
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide && World.getBlockEntity(Pos) instanceof SatelliteTerminalBlockEntity) {
            Scp179ClientHooks.openTerminal(Pos);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.satellite_terminal.tooltip").withStyle(ChatFormatting.GRAY));
        List<ResourceLocation> Stored = Stack.get(Scp179Registry.TerminalLocations.get());
        if (Stored != null && !Stored.isEmpty()) {
            Lines.add(Component.translatable("block.scp_unleashed.satellite_terminal.stored", Stored.size()).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.satellite_terminal.uplink").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.satellite_terminal.locations", SatelliteTerminalBlockEntity.MaxLocations).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.satellite_terminal.keeps").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.satellite_terminal.wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.network_object.draw", SatelliteTerminalBlockEntity.Draw).withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new SatelliteTerminalBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide) {
            return null;
        }
        return (TickWorld, Pos, TickState, Entity) -> {
            if (TickWorld instanceof ServerLevel Server && Entity instanceof SatelliteTerminalBlockEntity Terminal) {
                Terminal.serverTick(Server, TickState);
            }
        };
    }
}
