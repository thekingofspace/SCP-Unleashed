package net.rai.scp_unleashed.grenade;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.guns.GunGlass;

import java.util.Optional;

public final class GrenadeSight {
    public static final double Nudge = 1.0E-3;

    private GrenadeSight() {
    }

    public static boolean seeThrough(BlockState State) {
        if (State.is(Blocks.TINTED_GLASS)) {
            return false;
        }
        return State.is(Tags.Blocks.GLASS_BLOCKS) || State.is(Tags.Blocks.GLASS_PANES) || State.is(GunGlass.Bulletproof);
    }

    public static boolean lightReaches(Level World, Entity Source, Vec3 From, Vec3 To) {
        Vec3 Heading = To.subtract(From);
        double Length = Heading.length();
        if (Length < 1.0E-4) {
            return true;
        }
        Heading = Heading.scale(1.0 / Length);
        Vec3 Start = From;
        for (int Layer = 0; Layer < GrenadeTuning.MaxSightLayers; Layer++) {
            BlockHitResult Hit = World.clip(new ClipContext(Start, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Source));
            if (Hit.getType() != HitResult.Type.BLOCK) {
                return true;
            }
            BlockPos Pos = Hit.getBlockPos();
            if (!seeThrough(World.getBlockState(Pos))) {
                return false;
            }
            Optional<Vec3> Exit = new AABB(Pos).clip(To, Hit.getLocation());
            if (Exit.isEmpty()) {
                return true;
            }
            Start = Exit.get().add(Heading.scale(Nudge));
            if (Start.distanceToSqr(From) >= Length * Length) {
                return true;
            }
        }
        return false;
    }

    public static boolean clear(Level World, Entity Source, Vec3 From, Vec3 To) {
        BlockHitResult Hit = World.clip(new ClipContext(From, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Source));
        return Hit.getType() != HitResult.Type.BLOCK;
    }

    public static boolean reaches(Level World, Entity Source, Vec3 From, BlockPos Target) {
        BlockHitResult Hit = World.clip(new ClipContext(From, Vec3.atCenterOf(Target), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Source));
        return Hit.getType() != HitResult.Type.BLOCK || Hit.getBlockPos().equals(Target);
    }
}
