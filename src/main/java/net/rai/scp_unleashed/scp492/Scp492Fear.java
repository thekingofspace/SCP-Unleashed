package net.rai.scp_unleashed.scp492;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Scp492Fear {
    public static final double BlazeRadius = 5.0;
    public static final double HeldRadius = 3.5;
    public static final int Vertical = 2;

    private Scp492Fear() {
    }

    public static boolean frightening(LivingEntity Bearer) {
        if (Bearer instanceof Player User && User.isSpectator()) {
            return false;
        }
        return Bearer.getItemInHand(InteractionHand.MAIN_HAND).is(Scp492Registry.Frightening) || Bearer.getItemInHand(InteractionHand.OFF_HAND).is(Scp492Registry.Frightening);
    }

    public static boolean burning(BlockState State) {
        return State.is(BlockTags.FIRE) || CampfireBlock.isLitCampfire(State) || State.getFluidState().is(FluidTags.LAVA);
    }

    @Nullable
    public static Vec3 threat(Entity Figure) {
        Level World = Figure.level();
        Vec3 Here = Figure.position();
        Vec3 Best = null;
        double BestDistance = Double.MAX_VALUE;
        int Reach = (int) Math.ceil(BlazeRadius);
        BlockPos Centre = Figure.blockPosition();
        for (BlockPos Pos : BlockPos.betweenClosed(Centre.offset(-Reach, -Vertical, -Reach), Centre.offset(Reach, Vertical, Reach))) {
            Vec3 Spot = Vec3.atCenterOf(Pos);
            double Distance = Spot.distanceToSqr(Here);
            if (Distance > BlazeRadius * BlazeRadius || Distance >= BestDistance || !World.isLoaded(Pos)) {
                continue;
            }
            if (burning(World.getBlockState(Pos))) {
                Best = Spot;
                BestDistance = Distance;
            }
        }
        for (Entity Other : World.getEntities(Figure, Figure.getBoundingBox().inflate(BlazeRadius))) {
            double Distance = Other.distanceToSqr(Figure);
            if (Distance >= BestDistance || !Other.isAlive()) {
                continue;
            }
            boolean Flaming = Other.isOnFire() || Other instanceof Blaze;
            boolean Holding = Other instanceof LivingEntity Living && Distance <= HeldRadius * HeldRadius && frightening(Living);
            if ((Flaming && Distance <= BlazeRadius * BlazeRadius) || Holding) {
                Best = Other.position();
                BestDistance = Distance;
            }
        }
        return Best;
    }
}
