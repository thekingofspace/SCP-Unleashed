package net.rai.scp_unleashed.scp465;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.DClassLike;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public class Scp465GuestBrain extends Goal {
    public static final double ArriveDistance = 0.55;
    public static final double ArriveHeight = 1.2;
    public static final int RepathTicks = 20;
    public static final int GiveUpTicks = 160;
    public static final float TurnRate = 16.0f;

    private final Scp465Guest Guest;
    private int Repath;
    private int Walking;
    @Nullable
    private Vec3 LastSpot;

    public Scp465GuestBrain(Scp465Guest Guest) {
        this.Guest = Guest;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return Guest.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return Guest.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        Guest.getNavigation().stop();
        Guest.setAction(DClassLike.ActionNone);
    }

    @Override
    public void tick() {
        if (!(Guest.level() instanceof ServerLevel World)) {
            return;
        }
        if (Guest.flashing()) {
            Guest.getNavigation().stop();
            face(World, Guest.flashFace(), null);
            Guest.setAction(Guest.flashAction());
            return;
        }
        Vec3 Spot = Guest.spot();
        if (Spot == null) {
            Guest.setAction(DClassLike.ActionNone);
            return;
        }
        if (!Spot.equals(LastSpot)) {
            LastSpot = Spot;
            Walking = 0;
            Repath = 0;
        }
        double Dx = Spot.x - Guest.getX();
        double Dz = Spot.z - Guest.getZ();
        boolean Arrived = Dx * Dx + Dz * Dz < ArriveDistance * ArriveDistance && Math.abs(Spot.y - Guest.getY()) < ArriveHeight;
        if (!Arrived && Walking < GiveUpTicks) {
            Walking++;
            Guest.setAction(DClassLike.ActionNone);
            if (--Repath <= 0 || Guest.getNavigation().isDone()) {
                Guest.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, Scp465Celebration.WalkSpeed);
                Repath = RepathTicks;
            }
            return;
        }
        Guest.getNavigation().stop();
        face(World, Guest.faceId(), Guest.faceAt());
        Guest.setAction(Guest.taskAction());
    }

    private void face(ServerLevel World, @Nullable UUID Target, @Nullable Vec3 Point) {
        Vec3 Look = Point;
        if (Target != null) {
            Entity Found = World.getEntity(Target);
            if (Found != null) {
                Look = Found.getEyePosition();
            }
        }
        if (Look == null) {
            return;
        }
        double Dx = Look.x - Guest.getX();
        double Dz = Look.z - Guest.getZ();
        if (Dx * Dx + Dz * Dz < 1.0e-4) {
            return;
        }
        float Wanted = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        float Turned = Mth.approachDegrees(Guest.getYRot(), Wanted, TurnRate);
        Guest.setYRot(Turned);
        Guest.setYBodyRot(Turned);
        Guest.getLookControl().setLookAt(Look.x, Look.y, Look.z, 30.0f, 30.0f);
    }
}
