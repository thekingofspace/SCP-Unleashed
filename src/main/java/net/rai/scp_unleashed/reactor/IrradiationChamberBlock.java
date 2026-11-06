package net.rai.scp_unleashed.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IrradiationChamberBlock extends BaseEntityBlock {
    public static final MapCodec<IrradiationChamberBlock> BlockCodec = simpleCodec(IrradiationChamberBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Active = BooleanProperty.create("active");
    public static final int ActiveLight = 6;

    public IrradiationChamberBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Active, false));
    }

    @Nullable
    public static IItemHandler itemHandlerAt(Level World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof IrradiationChamberBlockEntity Chamber ? Chamber.getAutomation() : null;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Active);
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
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof IrradiationChamberBlockEntity Chamber) {
            User.openMenu(Chamber);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide && World.getBlockEntity(Pos) instanceof IrradiationChamberBlockEntity Chamber) {
            Chamber.dropContents(World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof IrradiationChamberBlockEntity Chamber ? ItemHandlerHelper.calcRedstoneFromInventory(Chamber.getContents()) : 0;
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Active)) {
            return;
        }
        Direction Front = State.getValue(Facing);
        if (Random.nextInt(3) == 0) {
            double X = Pos.getX() + 0.5 + Front.getStepX() * 0.53 + (Random.nextDouble() - 0.5) * 0.3 * Math.abs(Front.getStepZ());
            double Z = Pos.getZ() + 0.5 + Front.getStepZ() * 0.53 + (Random.nextDouble() - 0.5) * 0.3 * Math.abs(Front.getStepX());
            World.addParticle(ParticleTypes.GLOW, X, Pos.getY() + 0.35 + Random.nextDouble() * 0.3, Z, Front.getStepX() * 0.01, 0.01, Front.getStepZ() * 0.01);
        }
        if (Random.nextInt(50) == 0) {
            World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, ReactorRegistry.ChamberHum.get(), SoundSource.BLOCKS, 0.4f, 1.0f, false);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.irradiation_chamber.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.irradiation_chamber.detail.1").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.irradiation_chamber.detail.2").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.irradiation_chamber.detail.3").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new IrradiationChamberBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, ReactorRegistry.ChamberEntity.get(), IrradiationChamberBlockEntity::serverTick);
    }
}
