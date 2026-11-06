package net.rai.scp_unleashed.scp372;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Scp372AnchorBlock extends BaseEntityBlock {
    public static final MapCodec<Scp372AnchorBlock> BlockCodec = simpleCodec(Scp372AnchorBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Scp372AnchorLight> Light = EnumProperty.create("light", Scp372AnchorLight.class);
    public static final String Key = "block.scp_unleashed." + Scp372Registry.AnchorId + ".";
    public static final int GlintChance = 3;
    public static final DustParticleOptions Ember = new DustParticleOptions(new Vector3f(0.95f, 0.62f, 0.18f), 0.6f);
    private static final VoxelShape Plinth = Shapes.or(box(1.0, 0.0, 1.0, 15.0, 4.0, 15.0), box(5.0, 4.0, 5.0, 11.0, 10.0, 11.0));
    private static final Map<Direction, VoxelShape> Shapes0 = new EnumMap<>(Direction.class);

    static {
        Shapes0.put(Direction.NORTH, Shapes.or(Plinth, box(3.0, 10.0, 4.0, 13.0, 15.0, 12.0), box(5.0, 10.5, 3.0, 11.0, 14.5, 4.0)).optimize());
        Shapes0.put(Direction.SOUTH, Shapes.or(Plinth, box(3.0, 10.0, 4.0, 13.0, 15.0, 12.0), box(5.0, 10.5, 12.0, 11.0, 14.5, 13.0)).optimize());
        Shapes0.put(Direction.WEST, Shapes.or(Plinth, box(4.0, 10.0, 3.0, 12.0, 15.0, 13.0), box(3.0, 10.5, 5.0, 4.0, 14.5, 11.0)).optimize());
        Shapes0.put(Direction.EAST, Shapes.or(Plinth, box(4.0, 10.0, 3.0, 12.0, 15.0, 13.0), box(12.0, 10.5, 5.0, 13.0, 14.5, 11.0)).optimize());
    }

    public Scp372AnchorBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Light, Scp372AnchorLight.Off));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Light);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes0.get(State.getValue(Facing));
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Shapes.block();
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState State) {
        return true;
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
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && User instanceof ServerPlayer Server && World.getBlockEntity(Pos) instanceof Scp372AnchorBlockEntity Anchor) {
            Anchor.use(Server);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            WireNetworkManager.get(ServerWorld).markDirty();
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld) {
            if (World.getBlockEntity(Pos) instanceof Scp372AnchorBlockEntity Anchor) {
                Anchor.broken();
            }
            WireNetworkManager.get(ServerWorld).markDirty();
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp372AnchorBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp372Registry.AnchorEntity.get(), Scp372AnchorBlockEntity::tick);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        Scp372AnchorLight Lit = State.getValue(Light);
        if (Lit == Scp372AnchorLight.Off || Random.nextInt(GlintChance) != 0) {
            return;
        }
        Direction Front = State.getValue(Facing);
        Vec3 Lens = Vec3.atLowerCornerOf(Pos).add(0.5 + Front.getStepX() * Scp372AnchorBlockEntity.LensReach, Scp372AnchorBlockEntity.LensHeight,
                0.5 + Front.getStepZ() * Scp372AnchorBlockEntity.LensReach);
        double Spread = 0.12;
        double X = Lens.x + (Random.nextDouble() - 0.5) * Spread;
        double Y = Lens.y + (Random.nextDouble() - 0.5) * Spread;
        double Z = Lens.z + (Random.nextDouble() - 0.5) * Spread;
        if (Lit == Scp372AnchorLight.Holding) {
            World.addParticle(Scp372Registry.Flare.get(), X, Y, Z, Front.getStepX() * 0.02, 0.004, Front.getStepZ() * 0.02);
        } else if (Random.nextInt(4) == 0) {
            World.addParticle(Ember, X, Y, Z, Front.getStepX() * 0.01, 0.01, Front.getStepZ() * 0.01);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.DARK_AQUA));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "details").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Key + "draw", Scp372AnchorBlockEntity.PowerDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "lights").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Key + "dark").withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
