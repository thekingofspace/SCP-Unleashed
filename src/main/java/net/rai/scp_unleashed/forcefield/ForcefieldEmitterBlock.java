package net.rai.scp_unleashed.forcefield;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class ForcefieldEmitterBlock extends BaseEntityBlock {
    public static final MapCodec<ForcefieldEmitterBlock> BlockCodec = simpleCodec(ForcefieldEmitterBlock::new);
    public static final EnumProperty<Half> HalfPart = BlockStateProperties.HALF;
    public static final EnumProperty<Direction.Axis> Axis = BlockStateProperties.HORIZONTAL_AXIS;
    public static final IntegerProperty Glow = IntegerProperty.create("glow", 0, 3);
    public static final int GlowOff = 0;
    public static final int GlowBooting = 1;
    public static final int GlowOnline = 2;
    public static final int GlowFault = 3;
    public static final String Key = "block.scp_unleashed." + ForcefieldRegistry.EmitterId + ".";
    public static final String MessageKey = "message.scp_unleashed." + ForcefieldRegistry.EmitterId + ".";
    private static final VoxelShape BottomX = Shapes.or(Shapes.block(), Block.box(1.0, 16.0, 5.0, 15.0, 18.0, 11.0));
    private static final VoxelShape BottomZ = Shapes.or(Shapes.block(), Block.box(5.0, 16.0, 1.0, 11.0, 18.0, 15.0));
    private static final VoxelShape TopX = Shapes.or(Shapes.block(), Block.box(1.0, -2.0, 5.0, 15.0, 0.0, 11.0));
    private static final VoxelShape TopZ = Shapes.or(Shapes.block(), Block.box(5.0, -2.0, 1.0, 11.0, 0.0, 15.0));

    public ForcefieldEmitterBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(HalfPart, Half.BOTTOM).setValue(Axis, Direction.Axis.X).setValue(Glow, GlowOff));
    }

    public static int lightFor(int Shown) {
        return switch (Shown) {
            case GlowOnline -> 9;
            case GlowBooting -> 4;
            default -> 0;
        };
    }

    public static boolean isBottom(BlockState State) {
        return State.getBlock() instanceof ForcefieldEmitterBlock && State.getValue(HalfPart) == Half.BOTTOM;
    }

    public static boolean isTop(BlockState State) {
        return State.getBlock() instanceof ForcefieldEmitterBlock && State.getValue(HalfPart) == Half.TOP;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(HalfPart, Axis, Glow);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        boolean AlongX = State.getValue(Axis) == Direction.Axis.X;
        return State.getValue(HalfPart) == Half.BOTTOM ? (AlongX ? BottomX : BottomZ) : (AlongX ? TopX : TopZ);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        Direction Face = Context.getClickedFace();
        boolean Top = Face == Direction.DOWN || (Face != Direction.UP && Context.getClickLocation().y - Pos.getY() > 0.5);
        Half Part = Top ? Half.TOP : Half.BOTTOM;
        Direction.Axis Chosen = Context.getHorizontalDirection().getClockWise().getAxis();
        Direction.Axis Neighbour = neighbourAxis(World, Pos, Part);
        Direction.Axis Opposite = partnerAxis(World, Pos, Part);
        if (Opposite != null) {
            Chosen = Opposite;
        } else if (Neighbour != null) {
            Chosen = Neighbour;
        }
        return defaultBlockState().setValue(HalfPart, Part).setValue(Axis, Chosen);
    }

    @Nullable
    private static Direction.Axis neighbourAxis(Level World, BlockPos Pos, Half Part) {
        boolean AlongX = alignedAt(World, Pos.east(), Part, Direction.Axis.X) || alignedAt(World, Pos.west(), Part, Direction.Axis.X);
        boolean AlongZ = alignedAt(World, Pos.north(), Part, Direction.Axis.Z) || alignedAt(World, Pos.south(), Part, Direction.Axis.Z);
        if (AlongX == AlongZ) {
            return null;
        }
        return AlongX ? Direction.Axis.X : Direction.Axis.Z;
    }

    private static boolean alignedAt(Level World, BlockPos Pos, Half Part, Direction.Axis Wanted) {
        BlockState State = World.getBlockState(Pos);
        return State.getBlock() instanceof ForcefieldEmitterBlock && State.getValue(HalfPart) == Part && State.getValue(Axis) == Wanted;
    }

    @Nullable
    private static Direction.Axis partnerAxis(Level World, BlockPos Pos, Half Part) {
        Direction Toward = Part == Half.BOTTOM ? Direction.UP : Direction.DOWN;
        for (int Step = 1; Step <= ForcefieldEmitterBlockEntity.MaxGap + 1; Step++) {
            BlockState State = World.getBlockState(Pos.relative(Toward, Step));
            if (State.getBlock() instanceof ForcefieldEmitterBlock) {
                return State.getValue(HalfPart) != Part && Step > 1 ? State.getValue(Axis) : null;
            }
            if (!State.isAir() && !State.canBeReplaced()) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    public static ForcefieldEmitterBlockEntity bottomFor(Level World, BlockPos Pos, BlockState State) {
        if (isBottom(State)) {
            return World.getBlockEntity(Pos) instanceof ForcefieldEmitterBlockEntity Emitter ? Emitter : null;
        }
        if (!isTop(State)) {
            return null;
        }
        for (int Step = 1; Step <= ForcefieldEmitterBlockEntity.MaxGap + 1; Step++) {
            BlockPos Below = Pos.below(Step);
            BlockState Found = World.getBlockState(Below);
            if (Found.getBlock() instanceof ForcefieldEmitterBlock) {
                return isBottom(Found) && Found.getValue(Axis) == State.getValue(Axis) && World.getBlockEntity(Below) instanceof ForcefieldEmitterBlockEntity Emitter ? Emitter : null;
            }
            if (!Found.isAir() && !Found.is(ForcefieldRegistry.Barrier.get()) && !Found.canBeReplaced()) {
                return null;
            }
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!(World instanceof ServerLevel Server)) {
            return InteractionResult.SUCCESS;
        }
        ForcefieldEmitterBlockEntity Emitter = bottomFor(World, Pos, State);
        ForcefieldEmitterBlockEntity.Reboot Result = Emitter == null ? ForcefieldEmitterBlockEntity.Reboot.Unpaired : Emitter.reboot(Server);
        ChatFormatting Colour = switch (Result) {
            case Started -> ChatFormatting.AQUA;
            case AlreadyOnline, InProgress -> ChatFormatting.GRAY;
            default -> ChatFormatting.GOLD;
        };
        User.displayClientMessage(Component.translatable(MessageKey + Result.name().toLowerCase(Locale.ROOT)).withStyle(Colour), true);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Changed, BlockPos From, boolean Moving) {
        super.neighborChanged(State, World, Pos, Changed, From, Moving);
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        if (isBottom(State) && World.getBlockEntity(Pos) instanceof ForcefieldEmitterBlockEntity Emitter) {
            Emitter.markScanDirty();
            Emitter.redstone(Server, World.hasNeighborSignal(Pos));
        } else if (isTop(State)) {
            ForcefieldEmitterBlockEntity Emitter = bottomFor(World, Pos, State);
            if (Emitter != null) {
                Emitter.markPairDirty();
            }
        }
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (OldState.is(this) || !(World instanceof ServerLevel Server)) {
            return;
        }
        if (isBottom(State)) {
            WireNetworkManager.get(Server).markDirty();
        } else {
            ForcefieldEmitterBlockEntity Emitter = bottomFor(World, Pos, State);
            if (Emitter != null) {
                Emitter.markPairDirty();
            }
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel Server) {
            if (isBottom(State)) {
                if (World.getBlockEntity(Pos) instanceof ForcefieldEmitterBlockEntity Emitter) {
                    Emitter.dismantle(World);
                }
                WireNetworkManager.get(Server).markDirty();
            } else {
                ForcefieldEmitterBlockEntity Emitter = bottomFor(World, Pos, State);
                if (Emitter != null) {
                    Emitter.markPairDirty();
                }
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return isBottom(State) ? new ForcefieldEmitterBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (!isBottom(State)) {
            return null;
        }
        return createTickerHelper(Type, ForcefieldRegistry.EmitterEntity.get(), World.isClientSide ? ForcefieldEmitterBlockEntity::clientTick : ForcefieldEmitterBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        int Shown = State.getValue(Glow);
        if ((Shown != GlowBooting && Shown != GlowFault) || Random.nextInt(Shown == GlowBooting ? 2 : 6) != 0) {
            return;
        }
        boolean Bottom = State.getValue(HalfPart) == Half.BOTTOM;
        boolean AlongX = State.getValue(Axis) == Direction.Axis.X;
        double Along = 0.15 + Random.nextDouble() * 0.7;
        double X = Pos.getX() + (AlongX ? Along : 0.5);
        double Z = Pos.getZ() + (AlongX ? 0.5 : Along);
        double Y = Pos.getY() + (Bottom ? 1.14 : -0.14);
        if (Shown == GlowBooting) {
            World.addParticle(ParticleTypes.ELECTRIC_SPARK, X, Y, Z, 0.0, Bottom ? 0.06 : -0.06, 0.0);
        } else {
            World.addParticle(ParticleTypes.SMOKE, X, Y, Z, 0.0, Bottom ? 0.01 : -0.01, 0.0);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.AQUA));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "details").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Key + "pairing", ForcefieldEmitterBlockEntity.MaxGap).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "walls", ForcefieldEmitterBlockEntity.MaxColumns).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "draw", ForcefieldEmitterBlockEntity.BaseDraw, ForcefieldEmitterBlockEntity.CellDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "reboot").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "warning").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
