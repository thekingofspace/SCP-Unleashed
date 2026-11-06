package net.rai.scp_unleashed.scp999;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.security.TurretTargeting;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

public final class Scp999Danger {
    public static final double Radius = 8.0;
    public static final double BurningRadius = 4.0;
    public static final int FireReach = 3;
    public static final int FireVertical = 2;
    public static final double ExplosionRadius = 16.0;
    public static final int ExplosionFear = 100;

    private Scp999Danger() {
    }

    public static boolean burning(BlockState State) {
        return State.is(BlockTags.FIRE) || CampfireBlock.isLitCampfire(State) || State.getFluidState().is(FluidTags.LAVA);
    }

    public static boolean hostile(Entity Other) {
        if (!(Other instanceof LivingEntity Living) || !Living.isAlive() || Other instanceof Scp999Blob || Other instanceof Player || !Targeting.isTargetable(Other)) {
            return false;
        }
        if (Other instanceof NeutralMob Neutral && !Neutral.isAngry()) {
            return Other instanceof Mob Hunter && Scp999Goals.aggressive(Hunter);
        }
        if (Other instanceof Enemy) {
            return true;
        }
        if (Other instanceof Mob Hunter && Scp999Goals.aggressive(Hunter)) {
            return true;
        }
        return TurretTargeting.isAnomaly(BuiltInRegistries.ENTITY_TYPE.getKey(Other.getType())) && Other instanceof Mob Hunter && Hunter.getTarget() != null;
    }

    public static boolean explosive(Entity Other) {
        return Other instanceof PrimedTnt || Other instanceof MinecartTNT Cart && Cart.isPrimed();
    }

    @Nullable
    public static Vec3 threat(Scp999Blob Blob) {
        Level World = Blob.level();
        Vec3 Here = Blob.position();
        if (Blob.isOnFire()) {
            return Here;
        }
        Vec3 Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Entity Other : World.getEntities(Blob, Blob.getBoundingBox().inflate(Radius))) {
            double Distance = Other.distanceToSqr(Blob);
            if (Distance >= BestDistance || Distance > Radius * Radius) {
                continue;
            }
            boolean Flaming = Other.isOnFire() && Other instanceof LivingEntity && Distance <= BurningRadius * BurningRadius;
            if (hostile(Other) || explosive(Other) || Flaming) {
                Best = Other.position();
                BestDistance = Distance;
            }
        }
        BlockPos Centre = Blob.blockPosition();
        for (BlockPos Pos : BlockPos.betweenClosed(Centre.offset(-FireReach, -FireVertical, -FireReach), Centre.offset(FireReach, FireVertical, FireReach))) {
            Vec3 Spot = Vec3.atCenterOf(Pos);
            double Distance = Spot.distanceToSqr(Here);
            if (Distance >= BestDistance || Distance > (FireReach + 0.5) * (FireReach + 0.5) || !World.isLoaded(Pos)) {
                continue;
            }
            if (burning(World.getBlockState(Pos))) {
                Best = Spot;
                BestDistance = Distance;
            }
        }
        return Best;
    }
}
