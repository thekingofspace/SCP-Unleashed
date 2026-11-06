package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

public class Scp3008SealedBlock extends Block implements LiquidBlockContainer {
    public static final MapCodec<Scp3008SealedBlock> BlockCodec = simpleCodec(Scp3008SealedBlock::new);
    public static final float Resistance = 3600000.0f;

    public Scp3008SealedBlock(Properties Settings) {
        super(Settings);
    }

    public static BlockBehaviour.Properties sealed(MapColor Colour) {
        return BlockBehaviour.Properties.of().mapColor(Colour).strength(-1.0f, Resistance).pushReaction(PushReaction.BLOCK).noLootTable()
                .isValidSpawn((State, World, Pos, Kind) -> true);
    }

    @Override
    protected MapCodec<? extends Scp3008SealedBlock> codec() {
        return BlockCodec;
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
        return Resistance;
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
    protected boolean canBeReplaced(BlockState State, Fluid Liquid) {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState State, BlockPlaceContext Context) {
        return false;
    }
}
