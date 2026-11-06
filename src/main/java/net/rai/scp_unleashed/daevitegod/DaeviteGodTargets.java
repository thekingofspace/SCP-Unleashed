package net.rai.scp_unleashed.daevitegod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp140.api.DaeviteTags;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalDouble;

public final class DaeviteGodTargets {
    private DaeviteGodTargets() {
    }

    public static boolean isAlly(@Nullable Entity Found) {
        return Found instanceof DaeviteGod || Found instanceof DaeviteGodSpirit || Found != null && Found.getType().is(DaeviteTags.Allies);
    }

    public static boolean isPrey(Entity Hunter, @Nullable Entity Found) {
        if (!(Found instanceof LivingEntity Body) || Body == Hunter || isAlly(Body)) {
            return false;
        }
        if (!Body.isAlive() || Body.isRemoved() || Body.isDeadOrDying() || Body.isSpectator() || !Targeting.isTargetable(Body)) {
            return false;
        }
        if (Body instanceof Player User) {
            return !User.isCreative() && Hunter.level().getDifficulty() != Difficulty.PEACEFUL && !Scp268.isNeglected(User);
        }
        return ControlBodies.isPersonLike(Body);
    }

    public static boolean canHurt(Entity Attacker, @Nullable Entity Found) {
        if (!(Found instanceof LivingEntity Body) || Body == Attacker || isAlly(Body) || Body instanceof ArmorStand) {
            return false;
        }
        if (!Body.isAlive() || Body.isSpectator()) {
            return false;
        }
        return !(Body instanceof Player User) || !User.isCreative();
    }

    public static OptionalDouble groundAt(ServerLevel World, double X, double Z, double Around, int Below) {
        BlockPos Pos = BlockPos.containing(X, Around + 2.0, Z);
        int Lowest = Mth.floor(Around) - Below;
        while (Pos.getY() >= Lowest && !World.isOutsideBuildHeight(Pos)) {
            BlockPos Under = Pos.below();
            BlockState State = World.getBlockState(Under);
            if (State.isFaceSturdy(World, Under, Direction.UP) || !State.getCollisionShape(World, Under).isEmpty() && State.getCollisionShape(World, Under).max(Direction.Axis.Y) >= 0.5) {
                double Offset = 0.0;
                if (!World.isEmptyBlock(Pos)) {
                    VoxelShape Shape = World.getBlockState(Pos).getCollisionShape(World, Pos);
                    if (!Shape.isEmpty()) {
                        Offset = Shape.max(Direction.Axis.Y);
                    }
                }
                if (Offset < 1.0) {
                    VoxelShape Top = State.getCollisionShape(World, Under);
                    double Height = Top.isEmpty() ? 1.0 : Top.max(Direction.Axis.Y);
                    return OptionalDouble.of(Offset > 0.0 ? Pos.getY() + Offset : Under.getY() + Height);
                }
            }
            Pos = Under;
        }
        return OptionalDouble.empty();
    }
}
