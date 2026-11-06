package net.rai.scp_unleashed.scp457;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.rai.scp_unleashed.scp006.Scp006Registry;

public final class Scp457Water {
    public static final double LookAhead = 0.7;
    public static final double CauldronFloor = 6.0 / 16.0;
    public static final double CauldronStep = 3.0 / 16.0;

    private Scp457Water() {
    }

    public static boolean wet(FluidState Fluid) {
        return !Fluid.isEmpty() && (Fluid.is(FluidTags.WATER) || Fluid.getFluidType() == NeoForgeMod.WATER_TYPE.value() || Fluid.getFluidType() == Scp006Registry.WaterType.get());
    }

    public static boolean touching(LivingEntity Body) {
        if (Body.isInWaterOrBubble()) {
            return true;
        }
        if (Body.isInFluidType((Type, Height) -> Type == NeoForgeMod.WATER_TYPE.value() || Type == Scp006Registry.WaterType.get())) {
            return true;
        }
        return inCauldron(Body.level(), Body.getBoundingBox()) || inWetBlock(Body.level(), Body.getBoundingBox());
    }

    public static boolean inCauldron(Level World, AABB Box) {
        BlockPos Low = BlockPos.containing(Box.minX, Box.minY - 0.01, Box.minZ);
        BlockPos High = BlockPos.containing(Box.maxX, Box.minY + 1.0, Box.maxZ);
        for (BlockPos Pos : BlockPos.betweenClosed(Low, High)) {
            BlockState State = World.getBlockState(Pos);
            if (State.getBlock() instanceof LayeredCauldronBlock && State.hasProperty(LayeredCauldronBlock.LEVEL) && State.getFluidState().isEmpty()
                    && State.is(Blocks.WATER_CAULDRON)) {
                double Surface = Pos.getY() + CauldronFloor + CauldronStep * State.getValue(LayeredCauldronBlock.LEVEL);
                if (Box.minY < Surface && Box.maxX > Pos.getX() + 0.125 && Box.minX < Pos.getX() + 0.875 && Box.maxZ > Pos.getZ() + 0.125 && Box.minZ < Pos.getZ() + 0.875) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean inWetBlock(Level World, AABB Box) {
        BlockPos Low = BlockPos.containing(Box.minX + 0.001, Box.minY + 0.001, Box.minZ + 0.001);
        BlockPos High = BlockPos.containing(Box.maxX - 0.001, Box.maxY - 0.001, Box.maxZ - 0.001);
        for (BlockPos Pos : BlockPos.betweenClosed(Low, High)) {
            FluidState Fluid = World.getFluidState(Pos);
            if (wet(Fluid) && Box.minY < Pos.getY() + Fluid.getHeight(World, Pos)) {
                return true;
            }
        }
        return false;
    }

    public static boolean rainedOn(LivingEntity Body) {
        Level World = Body.level();
        if (!World.isRaining()) {
            return false;
        }
        BlockPos Feet = Body.blockPosition();
        return World.isRainingAt(Feet) || World.isRainingAt(BlockPos.containing(Body.getX(), Body.getBoundingBox().maxY, Body.getZ()));
    }

    public static boolean rainCell(Level World, BlockPos Pos) {
        if (!World.isRaining() || !World.isLoaded(Pos)) {
            return false;
        }
        return World.isRainingAt(Pos) || World.isRainingAt(Pos.above(2));
    }

    public static boolean isWaterSource(DamageSource Source) {
        if (Source.getDirectEntity() instanceof ThrownPotion Thrown) {
            PotionContents Contents = Thrown.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            return Contents.is(Potions.WATER);
        }
        return false;
    }

    public static boolean holdsWater(LivingEntity Holder) {
        return isWaterItem(Holder.getMainHandItem()) || isWaterItem(Holder.getOffhandItem());
    }

    public static boolean isWaterItem(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return false;
        }
        if (Stack.getItem() instanceof BucketItem Bucket) {
            return Bucket.content.defaultFluidState().is(FluidTags.WATER);
        }
        PotionContents Contents = Stack.get(DataComponents.POTION_CONTENTS);
        return Contents != null && Contents.is(Potions.WATER);
    }

    public static boolean wouldWade(LivingEntity Body, Vec3 Input) {
        Vec3 Motion = Body.getDeltaMovement();
        double Forward = Input.z;
        double Strafe = Input.x;
        float Yaw = Body.getYRot() * Mth.DEG_TO_RAD;
        double HeadingX = Motion.x + (Strafe * Mth.cos(Yaw) - Forward * Mth.sin(Yaw)) * 0.2;
        double HeadingZ = Motion.z + (Forward * Mth.cos(Yaw) + Strafe * Mth.sin(Yaw)) * 0.2;
        double Length = Math.sqrt(HeadingX * HeadingX + HeadingZ * HeadingZ);
        if (Length < 1.0e-4) {
            return false;
        }
        AABB Box = Body.getBoundingBox();
        AABB Ahead = Box.move(HeadingX / Length * LookAhead, 0.0, HeadingZ / Length * LookAhead);
        AABB Probe = new AABB(Ahead.minX, Box.minY - 1.0, Ahead.minZ, Ahead.maxX, Box.minY + 1.2, Ahead.maxZ);
        Level World = Body.level();
        BlockPos Low = BlockPos.containing(Probe.minX, Probe.minY, Probe.minZ);
        BlockPos High = BlockPos.containing(Probe.maxX, Probe.maxY, Probe.maxZ);
        for (BlockPos Pos : BlockPos.betweenClosed(Low, High)) {
            if (wet(World.getFluidState(Pos))) {
                return true;
            }
        }
        return inCauldron(World, Ahead);
    }
}
