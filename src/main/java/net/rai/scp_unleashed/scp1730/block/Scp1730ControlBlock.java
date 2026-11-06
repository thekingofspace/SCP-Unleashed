package net.rai.scp_unleashed.scp1730.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp1730.Scp1730Director;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730WingDoor;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp1730.Scp1730World;

import java.util.EnumMap;
import java.util.Map;

public class Scp1730ControlBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp1730ControlBlock> ControlCodec = simpleCodec(Scp1730ControlBlock::new);
    public static final EnumProperty<Kind> Type = EnumProperty.create("kind", Kind.class);
    public static final BooleanProperty Active = BooleanProperty.create("active");
    private static final Map<Direction, VoxelShape> Panels = new EnumMap<>(Direction.class);

    static {
        Panels.put(Direction.NORTH, Block.box(2, 1, 12, 14, 15, 16));
        Panels.put(Direction.SOUTH, Block.box(2, 1, 0, 14, 15, 4));
        Panels.put(Direction.WEST, Block.box(12, 1, 2, 16, 15, 14));
        Panels.put(Direction.EAST, Block.box(0, 1, 2, 4, 15, 14));
    }

    public enum Kind implements StringRepresentable {
        Reader("reader"),
        Release("release"),
        Breaker("breaker"),
        Clearance("clearance"),
        Exit("exit");

        private final String Name;

        Kind(String Name) {
            this.Name = Name;
        }

        public static Kind byIndex(int Index) {
            Kind[] All = values();
            return All[Math.floorMod(Index, All.length)];
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    public Scp1730ControlBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Type, Kind.Breaker).setValue(Active, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return ControlCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Type, Active);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Face = Context.getClickedFace().getAxis().isHorizontal() ? Context.getClickedFace() : Context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, Face);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Panels.get(State.getValue(FACING));
    }

    @Override
    protected boolean isSignalSource(BlockState State) {
        return true;
    }

    @Override
    protected int getSignal(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        return State.getValue(Active) ? 15 : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel Server = (ServerLevel) World;
        Kind Which = State.getValue(Type);
        boolean Inside = Scp1730World.inside(World);
        switch (Which) {
            case Reader -> {
                if (State.getValue(Active)) {
                    return InteractionResult.CONSUME;
                }
                if (!User.getMainHandItem().is(Scp1730Registry.AccessCard.get()) && !User.getOffhandItem().is(Scp1730Registry.AccessCard.get())) {
                    World.playSound(null, Pos, Scp1730Registry.ReaderDeny.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
                    User.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.reader_denied").withStyle(ChatFormatting.RED), true);
                    return InteractionResult.CONSUME;
                }
                World.setBlock(Pos, State.setValue(Active, true), Block.UPDATE_ALL);
                World.playSound(null, Pos, Scp1730Registry.ReaderAccept.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
                if (Inside) {
                    Scp1730Director.controlUsed(Server, Pos, Which, true, (ServerPlayer) User);
                }
            }
            case Release -> {
                if (State.getValue(Active)) {
                    return InteractionResult.CONSUME;
                }
                World.setBlock(Pos, State.setValue(Active, true), Block.UPDATE_ALL);
                World.playSound(null, Pos, Scp1730Registry.Release.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
                if (Inside) {
                    Scp1730Director.controlUsed(Server, Pos, Which, true, (ServerPlayer) User);
                }
            }
            case Clearance -> {
                if (!State.getValue(Active)) {
                    ItemStack Card = User.getMainHandItem().is(Scp1730Registry.ClearanceCard.get()) ? User.getMainHandItem()
                            : User.getOffhandItem().is(Scp1730Registry.ClearanceCard.get()) ? User.getOffhandItem() : ItemStack.EMPTY;
                    if (Card.isEmpty()) {
                        World.playSound(null, Pos, Scp1730Registry.ReaderDeny.get(), SoundSource.BLOCKS, 0.8f, 0.8f);
                        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.clearance_denied").withStyle(ChatFormatting.RED), true);
                        return InteractionResult.CONSUME;
                    }
                    if (!User.getAbilities().instabuild) {
                        Card.shrink(1);
                    }
                    World.setBlock(Pos, State.setValue(Active, true), Block.UPDATE_ALL);
                    User.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.clearance_kept").withStyle(ChatFormatting.GREEN), true);
                }
                World.playSound(null, Pos, Scp1730Registry.ReaderAccept.get(), SoundSource.BLOCKS, 0.9f, 0.85f);
                if (Inside) {
                    Scp1730WingDoor.use(Server, Pos, (ServerPlayer) User);
                }
            }
            case Exit -> {
                World.playSound(null, Pos, Scp1730Registry.Release.get(), SoundSource.BLOCKS, 0.8f, 1.2f);
                if (Inside) {
                    Scp1730WingDoor.use(Server, Pos, (ServerPlayer) User);
                }
            }
            case Breaker -> {
                boolean Cut = !State.getValue(Active);
                World.setBlock(Pos, State.setValue(Active, Cut), Block.UPDATE_ALL);
                World.playSound(null, Pos, Scp1730Registry.Breaker.get(), SoundSource.BLOCKS, 1.0f, Cut ? 0.8f : 1.1f);
                if (Inside) {
                    Scp1730Director.controlUsed(Server, Pos, Which, Cut, (ServerPlayer) User);
                }
            }
        }
        return InteractionResult.CONSUME;
    }
}
