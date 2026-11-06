package net.rai.scp_unleashed.scp087;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

public class Scp087StairBlock extends StairBlock {
    public static final MapCodec<Scp087StairBlock> StairCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            BlockState.CODEC.fieldOf("base_state").forGetter(Stairs -> Stairs.baseState), propertiesCodec()).apply(Instance, Scp087StairBlock::new));

    public Scp087StairBlock(BlockState Base, Properties Settings) {
        super(Base, Settings);
    }

    @Override
    public MapCodec<? extends StairBlock> codec() {
        return StairCodec;
    }

    @Override
    public boolean canEntityDestroy(BlockState State, BlockGetter World, BlockPos Pos, Entity Breaker) {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState State) {
        return PushReaction.BLOCK;
    }

    @Override
    public boolean canDropFromExplosion(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return false;
    }

    @Override
    public void onBlockExploded(BlockState State, Level World, BlockPos Pos, Explosion Blast) {
    }

    @Override
    public float getExplosionResistance(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return Scp087SealedBlock.Resistance;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState State, Level World, BlockPos Pos, Player User, boolean WillHarvest, FluidState Liquid) {
        return User.isCreative() && super.onDestroyedByPlayer(State, World, Pos, User, WillHarvest, Liquid);
    }

    @Override
    public boolean isFlammable(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
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

    @Override
    protected FluidState getFluidState(BlockState State) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    protected boolean canBeReplaced(BlockState State, Fluid Liquid) {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState State, BlockPlaceContext Context) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Placed = super.getStateForPlacement(Context);
        return Placed == null ? null : Placed.setValue(WATERLOGGED, false);
    }
}
