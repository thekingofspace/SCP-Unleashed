package net.rai.scp_unleashed.scp5001.mekhane.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneIntake;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneProtocol;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneValues;
import net.rai.scp_unleashed.scp5001.mekhane.Scp5001MekhaneRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class Scp5001EmergencyIntakeBlock extends BaseEntityBlock {
    public static final MapCodec<Scp5001EmergencyIntakeBlock> BlockCodec = simpleCodec(Scp5001EmergencyIntakeBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty Awake = BooleanProperty.create("awake");
    private static final Map<Direction, VoxelShape> ShapeTable = new EnumMap<>(Direction.class);

    static {
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            ShapeTable.put(Side, Shapes.or(Scp5001EmergencyConsoleBlock.rotated(Side, 0, 0, 0, 16, 10, 16),
                    Scp5001EmergencyConsoleBlock.rotated(Side, 0, 10, 0, 2, 16, 16), Scp5001EmergencyConsoleBlock.rotated(Side, 14, 10, 0, 16, 16, 16),
                    Scp5001EmergencyConsoleBlock.rotated(Side, 2, 10, 14, 14, 16, 16)));
        }
    }

    public Scp5001EmergencyIntakeBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Awake, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Awake);
    }

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
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ShapeTable.get(State.getValue(Facing));
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (MekhaneValues.worth(Stack) == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        MinecraftServer Server = World.getServer();
        if (Server == null) {
            return ItemInteractionResult.CONSUME;
        }
        int Taken = MekhaneIntake.accept(Server, Stack, false);
        report(World, Pos, User, Taken, Stack, Server);
        if (Taken > 0 && !User.getAbilities().instabuild) {
            Stack.shrink(Taken);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        MinecraftServer Server = World.getServer();
        if (Server == null) {
            return InteractionResult.CONSUME;
        }
        if (!User.isShiftKeyDown()) {
            report(World, Pos, User, 0, ItemStack.EMPTY, Server);
            return InteractionResult.CONSUME;
        }
        Inventory Pack = User.getInventory();
        int Total = 0;
        for (int Slot = 0; Slot < Pack.getContainerSize(); Slot++) {
            ItemStack Each = Pack.getItem(Slot);
            if (Each.isEmpty() || MekhaneValues.worth(Each) == null) {
                continue;
            }
            int Taken = MekhaneIntake.accept(Server, Each, false);
            if (Taken > 0) {
                Total += Taken;
                Each.shrink(Taken);
            }
        }
        report(World, Pos, User, Total, ItemStack.EMPTY, Server);
        return InteractionResult.CONSUME;
    }

    private static void report(Level World, BlockPos Pos, Player User, int Taken, ItemStack Stack, MinecraftServer Server) {
        if (!(User instanceof ServerPlayer Player)) {
            return;
        }
        if (!MekhaneIntake.open(Server)) {
            World.playSound(null, Pos, Scp5001MekhaneRegistry.ConsoleDeny.get(), SoundSource.BLOCKS, 0.6f, 0.7f);
            Player.displayClientMessage(Component.translatable("message.scp_unleashed.scp5001_emergency.intake_sealed").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        if (Taken > 0) {
            if (World.getBlockEntity(Pos) instanceof Scp5001EmergencyIntakeBlockEntity Intake) {
                Intake.fed();
            }
        } else if (!Stack.isEmpty()) {
            World.playSound(null, Pos, Scp5001MekhaneRegistry.IntakeFull.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
        }
        Player.displayClientMessage(Component.translatable("message.scp_unleashed.scp5001_emergency.intake_progress",
                percent(Protocol.iron(), MekhaneValues.ironNeed()), percent(Protocol.electronics(), MekhaneValues.electronicsNeed())).withStyle(ChatFormatting.GOLD), true);
    }

    public static String percent(long Have, long Need) {
        double Share = Need <= 0 ? 1.0 : Math.min(1.0, Have / (double) Need);
        return String.format(java.util.Locale.ROOT, "%.1f", Share * 100.0);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp5001EmergencyIntakeBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp5001MekhaneRegistry.IntakeEntity.get(), Scp5001EmergencyIntakeBlockEntity::serverTick);
    }
}
