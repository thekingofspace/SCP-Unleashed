package net.rai.scp_unleashed.scp5001.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class Scp5001ShellBlock extends Block {
    public static final BooleanProperty Seam = BooleanProperty.create("seam");

    public Scp5001ShellBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Seam, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Seam);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState();
    }

    @Override
    public boolean canEntityDestroy(BlockState State, BlockGetter World, BlockPos Pos, Entity Breaker) {
        return false;
    }

    @Override
    public boolean canDropFromExplosion(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return false;
    }

    @Override
    public void onBlockExploded(BlockState State, Level World, BlockPos Pos, Explosion Blast) {
    }

    @Override
    public boolean canBeReplaced(BlockState State, net.minecraft.world.level.material.Fluid Fluid) {
        return false;
    }

    public static boolean immune(Entity Breaker) {
        return Breaker instanceof WitherBoss || Breaker instanceof EnderDragon;
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Scp5001WallFaces.occlusion(State);
    }

    @Override
    protected int getLightBlock(BlockState State, BlockGetter World, BlockPos Pos) {
        return World.getMaxLightLevel();
    }
}
