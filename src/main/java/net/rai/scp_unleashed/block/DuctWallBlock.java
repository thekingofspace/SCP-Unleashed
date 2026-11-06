package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.entity.DuctWallBlockEntity;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.DuctKind;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class DuctWallBlock extends DuctBlock {
    public static final MapCodec<DuctWallBlock> WallCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            DuctKind.KindCodec.fieldOf("kind").forGetter(DuctWallBlock::getKind),
            propertiesCodec()
    ).apply(Instance, DuctWallBlock::new));
    public static final EnumProperty<Direction.Axis> PortAxis = BlockStateProperties.AXIS;

    public DuctWallBlock(DuctKind Kind, Properties Properties) {
        super(Kind, Properties);
        registerDefaultState(defaultBlockState().setValue(PortAxis, Direction.Axis.Z));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return WallCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(PortAxis);
    }

    @Override
    public boolean acceptsConnection(BlockState State, Direction Side) {
        return true;
    }

    public static boolean showsPort(BlockState State, Direction Face) {
        if (State.hasProperty(PortAxis) && State.getValue(PortAxis) == Face.getAxis()) {
            return true;
        }
        BooleanProperty Linked = Connections.get(Face);
        return State.hasProperty(Linked) && State.getValue(Linked);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState State = defaultBlockState().setValue(PortAxis, Context.getClickedFace().getAxis());
        for (Map.Entry<Direction, BooleanProperty> Entry : Connections.entrySet()) {
            State = State.setValue(Entry.getValue(), connectsTo(World.getBlockState(Pos.relative(Entry.getKey())), Entry.getKey()));
        }
        return State;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        return State.setValue(Connections.get(Side), connectsTo(NeighborState, Side));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return false;
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player User, BlockGetter World, BlockPos Pos, BlockState State, Fluid Liquid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor World, BlockPos Pos, BlockState State, FluidState Liquid) {
        return false;
    }

    public static boolean canDisguiseAs(BlockGetter World, BlockPos Pos, BlockState Look) {
        return !(Look.getBlock() instanceof DuctBlock) && !Look.hasBlockEntity() && Look.getRenderShape() == RenderShape.MODEL
                && Look.isSolidRender(World, Pos) && Look.isCollisionShapeFullBlock(World, Pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!(Stack.getItem() instanceof BlockItem Placeable) || !(World.getBlockEntity(Pos) instanceof DuctWallBlockEntity Wall)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockState Look = Placeable.getBlock().defaultBlockState();
        if (!canDisguiseAs(World, Pos, Look)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (Look.equals(Wall.getDisguise())) {
            return ItemInteractionResult.CONSUME;
        }
        if (!World.isClientSide) {
            Wall.returnDisguise(User);
            boolean Paid = !User.getAbilities().instabuild;
            if (Paid) {
                Stack.shrink(1);
            }
            Wall.setDisguise(Look, Paid);
            SoundType Sound = Look.getSoundType(World, Pos, User);
            World.playSound(null, Pos, Sound.getPlaceSound(), SoundSource.BLOCKS, (Sound.getVolume() + 1.0f) / 2.0f, Sound.getPitch() * 0.8f);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!User.isShiftKeyDown() || !(World.getBlockEntity(Pos) instanceof DuctWallBlockEntity Wall) || !Wall.hasDisguise()) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            SoundType Sound = Wall.getDisguise().getSoundType(World, Pos, User);
            Wall.returnDisguise(User);
            Wall.clearDisguise();
            World.playSound(null, Pos, Sound.getBreakSound(), SoundSource.BLOCKS, (Sound.getVolume() + 1.0f) / 2.0f, Sound.getPitch() * 0.8f);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide && World.getBlockEntity(Pos) instanceof DuctWallBlockEntity Wall && Wall.isConsumed() && Wall.hasDisguise()) {
            Block.popResource(World, Pos, new ItemStack(Wall.getDisguise().getBlock()));
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed." + getKind().getSerializedName() + "_wall_port.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.wall_port.sides").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.wall_port.disguise").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.wall_port.clear").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new DuctWallBlockEntity(Pos, State);
    }
}
