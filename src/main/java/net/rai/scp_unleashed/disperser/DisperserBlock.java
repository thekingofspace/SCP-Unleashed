package net.rai.scp_unleashed.disperser;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.labware.LabwareSounds;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DisperserBlock extends BaseEntityBlock {
    public static final MapCodec<DisperserBlock> BlockCodec = simpleCodec(DisperserBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Active = BooleanProperty.create("active");
    public static final BooleanProperty Loaded = BooleanProperty.create("loaded");
    public static final VoxelShape Outline = Shapes.or(Block.box(0, 0, 0, 16, 12, 16), Block.box(3, 12, 3, 13, 14, 13), Block.box(5, 14, 5, 11, 15, 11),
            Block.box(6, 15, 6, 10, 16, 10));

    public DisperserBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Active, false).setValue(Loaded, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Active, Loaded);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
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
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter Getter, BlockPos Pos, CollisionContext Context) {
        return Outline;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter Getter, BlockPos Pos) {
        return Shapes.block();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Held, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!(World.getBlockEntity(Pos) instanceof DisperserBlockEntity Machine) || !Machine.accepts(Held)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            Item Used = Held.getItem();
            ItemStack Previous = Machine.swap(Held);
            if (!User.getAbilities().instabuild) {
                Held.shrink(1);
            }
            if (!Previous.isEmpty()) {
                if (Held.isEmpty()) {
                    User.setItemInHand(Hand, Previous);
                } else if (!User.getInventory().add(Previous)) {
                    User.drop(Previous, false);
                }
            }
            User.awardStat(Stats.ITEM_USED.get(Used));
            LabwareSounds.play(World, Pos, LabwareSounds.Racked, LabwareSounds.pitch(World));
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!(World.getBlockEntity(Pos) instanceof DisperserBlockEntity Machine) || !User.getMainHandItem().isEmpty() || !State.getValue(Loaded)) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            ItemStack Taken = Machine.take();
            if (!Taken.isEmpty()) {
                User.setItemInHand(InteractionHand.MAIN_HAND, Taken);
                LabwareSounds.play(World, Pos, LabwareSounds.Racked, LabwareSounds.pitch(World) * 0.9f);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World.getBlockEntity(Pos) instanceof DisperserBlockEntity Machine) {
            Machine.dropContents(World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Active) || !(World.getBlockEntity(Pos) instanceof DisperserBlockEntity Machine)) {
            return;
        }
        int Colour = LabOrganisms.mistColourOf(Machine.getCulture());
        Vec3 Nozzle = DisperserBlockEntity.nozzle(Pos).add(0.0, 0.1, 0.0);
        for (int Index = 0; Index < DisperserTuning.WispChance; Index++) {
            World.addParticle(DisperserSpread.haze(Colour), Nozzle.x + (Random.nextDouble() - 0.5) * DisperserTuning.WispSpread, Nozzle.y,
                    Nozzle.z + (Random.nextDouble() - 0.5) * DisperserTuning.WispSpread, (Random.nextDouble() - 0.5) * 0.02, 0.03 + Random.nextDouble() * 0.03,
                    (Random.nextDouble() - 0.5) * 0.02);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new DisperserBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, DisperserRegistry.DisperserEntity.get(), DisperserBlockEntity::serverTick);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.disperser.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.disperser.load").withStyle(ChatFormatting.AQUA));
            Lines.add(Component.translatable("block.scp_unleashed.disperser.unload").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.disperser.run").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.disperser.endless").withStyle(ChatFormatting.RED));
        }
    }
}
