package net.rai.scp_unleashed.processing;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ElectricFurnaceBlock extends ProcessingMachineBlock {
    public static final MapCodec<ElectricFurnaceBlock> BlockCodec = simpleCodec(ElectricFurnaceBlock::new);
    public static final int ActiveLight = 10;
    public static final float SmokeChance = 0.18f;

    public ElectricFurnaceBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected String getMachineName() {
        return "electric_furnace";
    }

    @Override
    protected int getWorkDraw() {
        return ElectricFurnaceBlockEntity.WorkDraw;
    }

    @Override
    protected void appendDetails(List<Component> Lines) {
        Lines.add(Component.translatable("block.scp_unleashed.electric_furnace.speed", ElectricFurnaceBlockEntity.SpeedFactor).withStyle(ChatFormatting.GOLD));
        Lines.add(Component.translatable("block.scp_unleashed.electric_furnace.special").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Active) || Random.nextFloat() > SmokeChance || !World.getBlockState(Pos.above()).isAir()) {
            return;
        }
        Direction Front = State.getValue(Facing);
        double Back = -0.22;
        double X = Pos.getX() + 0.5 + Front.getStepX() * Back + (Random.nextDouble() - 0.5) * 0.3;
        double Z = Pos.getZ() + 0.5 + Front.getStepZ() * Back + (Random.nextDouble() - 0.5) * 0.3;
        World.addParticle(ParticleTypes.SMOKE, X, Pos.getY() + 1.02, Z, 0.0, 0.03, 0.0);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new ElectricFurnaceBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide) {
            return createTickerHelper(Type, ProcessingRegistry.ElectricFurnaceEntity.get(), ProcessingMachineBlockEntity::clientTick);
        }
        return createTickerHelper(Type, ProcessingRegistry.ElectricFurnaceEntity.get(), ProcessingMachineBlockEntity::serverTick);
    }
}
