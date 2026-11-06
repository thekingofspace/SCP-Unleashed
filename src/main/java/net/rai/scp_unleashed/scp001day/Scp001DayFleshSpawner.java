package net.rai.scp_unleashed.scp001day;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayFlesh;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayForm;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayFusion;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySun;

public final class Scp001DayFleshSpawner {
    public static final int CheckTicks = 200;
    public static final int DayCap = 10;
    public static final int NightCap = 24;
    public static final double PlayerRange = 64.0;
    public static final int MinRange = 20;
    public static final int MaxRange = 40;
    public static final int Attempts = 12;

    private Scp001DayFleshSpawner() {
    }

    public static int cap(ServerLevel World) {
        return Scp001DaySun.lethal(Scp001DaySun.direction(World.getDayTime())) ? DayCap : NightCap;
    }

    public static Scp001DayForm pick(RandomSource Random, boolean Night) {
        int Roll = Random.nextInt(100);
        if (Night && Roll < 6) {
            return Scp001DayForm.Mass;
        }
        if (Roll < 46) {
            return Scp001DayForm.Crawler;
        }
        if (Roll < 70) {
            return Scp001DayForm.Clinger;
        }
        return Scp001DayForm.Lurker;
    }

    public static void tick(ServerLevel World) {
        if (World.getGameTime() % CheckTicks != 0) {
            return;
        }
        boolean Night = !Scp001DaySun.lethal(Scp001DaySun.direction(World.getDayTime()));
        for (ServerPlayer User : World.players()) {
            if (User.isSpectator() || User.isCreative()) {
                continue;
            }
            int Present = Scp001DayFusion.areaMass(World, User.position());
            Scp001DayForm Form = pick(World.random, Night);
            if (Present + Form.Weight > cap(World)) {
                continue;
            }
            spawnNear(World, User, Form);
        }
    }

    public static Scp001DayFlesh spawnNear(ServerLevel World, ServerPlayer User, Scp001DayForm Form) {
        boolean Day = Scp001DaySun.lethal(Scp001DaySun.direction(World.getDayTime()));
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = World.random.nextDouble() * Math.PI * 2.0;
            double Distance = MinRange + World.random.nextDouble() * (MaxRange - MinRange);
            int X = (int) Math.floor(User.getX() + Math.cos(Angle) * Distance);
            int Z = (int) Math.floor(User.getZ() + Math.sin(Angle) * Distance);
            BlockPos Feet = Scp001DayRifts.standing(World, X, Z, World.random);
            if (Feet == null || Math.abs(Feet.getY() - User.getY()) > 24) {
                continue;
            }
            if (Day && Scp001DayFlesh.lit(World, Feet)) {
                continue;
            }
            Vec3 Spot = Vec3.atBottomCenterOf(Feet);
            if (User.hasLineOfSight(new net.minecraft.world.entity.decoration.ArmorStand(World, Spot.x, Spot.y + 1.0, Spot.z))) {
                continue;
            }
            return Scp001DayFusion.make(World, Form.Weight, Spot, null);
        }
        return null;
    }
}
