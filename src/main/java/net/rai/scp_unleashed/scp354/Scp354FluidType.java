package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public class Scp354FluidType extends FluidType {
    public static final double SinkPull = 0.012;
    public static final double SinkLimit = -0.07;
    public static final double SinkDrag = 0.7;
    public static final double HeldCarry = 0.3;

    public Scp354FluidType(Properties Settings) {
        super(Settings);
    }

    @Override
    public boolean canConvertToSource(FluidState State, LevelReader Reader, BlockPos Pos) {
        return false;
    }

    @Override
    public boolean canConvertToSource(FluidStack Stack) {
        return false;
    }

    @Override
    public boolean isVaporizedOnPlacement(Level World, BlockPos Pos, FluidStack Stack) {
        return false;
    }

    @Override
    public ItemStack getBucket(FluidStack Stack) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canHydrate(Entity Target) {
        return false;
    }

    @Override
    public boolean move(FluidState State, LivingEntity Mover, Vec3 Input, double Gravity) {
        if (Mover instanceof Scp354Swimmer Swimmer && Swimmer.swimsOut()) {
            Scp354Swim.travel(Mover, Swimmer);
            return true;
        }
        if (!Scp354Grab.isLocked(Mover)) {
            return false;
        }
        Vec3 Motion = Mover.getDeltaMovement();
        Mover.move(MoverType.SELF, Motion);
        Mover.setDeltaMovement(Motion.scale(HeldCarry));
        return true;
    }

    @Override
    public void setItemMovement(ItemEntity Dropped) {
        Vec3 Motion = Dropped.getDeltaMovement();
        Dropped.setDeltaMovement(Motion.x * SinkDrag, Math.max(SinkLimit, Motion.y - SinkPull), Motion.z * SinkDrag);
    }
}
