package net.rai.scp_unleashed.cooler;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CoolerBlock extends BaseEntityBlock {
    public static final MapCodec<CoolerBlock> BlockCodec = simpleCodec(CoolerBlock::new);
    public static final BooleanProperty Powered = BlockStateProperties.POWERED;
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final String Key = "block.scp_unleashed." + CoolerRegistry.CoolerId + ".";
    public static final int MistChance = 2;

    public CoolerBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Powered, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Powered);
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
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
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
            WireNetworkManager.get(ServerWorld).markDirty();
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new CoolerBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, CoolerRegistry.CoolerEntity.get(), CoolerBlockEntity::tick);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Powered) || Random.nextInt(MistChance) != 0) {
            return;
        }
        BlockPos Above = Pos.above();
        if (!World.getBlockState(Above).isSolidRender(World, Above)) {
            double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * 0.4;
            double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * 0.4;
            World.addParticle(ParticleTypes.SNOWFLAKE, X, Pos.getY() + 1.02, Z, (Random.nextDouble() - 0.5) * 0.02, 0.02, (Random.nextDouble() - 0.5) * 0.02);
        }
        Direction Front = State.getValue(Facing);
        BlockPos Ahead = Pos.relative(Front);
        if (Random.nextInt(3) == 0 && !World.getBlockState(Ahead).isSolidRender(World, Ahead)) {
            double X = Pos.getX() + 0.5 + Front.getStepX() * 0.55 + (Front.getStepX() == 0 ? (Random.nextDouble() - 0.5) * 0.5 : 0.0);
            double Z = Pos.getZ() + 0.5 + Front.getStepZ() * 0.55 + (Front.getStepZ() == 0 ? (Random.nextDouble() - 0.5) * 0.5 : 0.0);
            World.addParticle(ParticleTypes.WHITE_ASH, X, Pos.getY() + 0.35 + Random.nextDouble() * 0.3, Z, Front.getStepX() * 0.02, -0.005, Front.getStepZ() * 0.02);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.AQUA));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "details").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Key + "radius", CoolerBlockEntity.radiusBlocks()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "draw", CoolerBlockEntity.PowerDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "wiring").withStyle(ChatFormatting.GOLD));
        }
    }
}
