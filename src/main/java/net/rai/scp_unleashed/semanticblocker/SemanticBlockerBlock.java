package net.rai.scp_unleashed.semanticblocker;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp148.Scp148Cue;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SemanticBlockerBlock extends BaseEntityBlock {
    public static final MapCodec<SemanticBlockerBlock> BlockCodec = simpleCodec(SemanticBlockerBlock::new);
    public static final BooleanProperty Powered = BlockStateProperties.POWERED;
    public static final String Key = "block.scp_unleashed." + SemanticBlockerRegistry.BlockerId + ".";
    public static final int GlintChance = 3;
    public static final double GlintSpread = 0.7;

    public SemanticBlockerBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Powered, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Powered);
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
        return new SemanticBlockerBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, SemanticBlockerRegistry.BlockerEntity.get(), SemanticBlockerBlockEntity::tick);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Powered) || Random.nextInt(GlintChance) != 0) {
            return;
        }
        Direction Side = Random.nextInt(3) == 0 ? Direction.UP : Direction.Plane.HORIZONTAL.getRandomDirection(Random);
        BlockPos Beside = Pos.relative(Side);
        if (World.getBlockState(Beside).isSolidRender(World, Beside)) {
            return;
        }
        double X = Pos.getX() + 0.5 + Side.getStepX() * 0.52 + (Side.getStepX() == 0 ? Random.nextDouble() - 0.5 : 0.0) * GlintSpread;
        double Y = Pos.getY() + 0.5 + Side.getStepY() * 0.52 + (Side.getStepY() == 0 ? Random.nextDouble() - 0.5 : 0.0) * GlintSpread;
        double Z = Pos.getZ() + 0.5 + Side.getStepZ() * 0.52 + (Side.getStepZ() == 0 ? Random.nextDouble() - 0.5 : 0.0) * GlintSpread;
        DustParticleOptions Speck = Random.nextInt(3) == 0 ? Scp148Cue.Shimmer : Scp148Cue.ShimmerDeep;
        World.addParticle(Speck, X, Y, Z, Side.getStepX() * 0.01, 0.012, Side.getStepZ() * 0.01);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GREEN));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "details").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Key + "radius", SemanticBlockerBlockEntity.radiusBlocks()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "draw", SemanticBlockerBlockEntity.PowerDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "dark").withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
