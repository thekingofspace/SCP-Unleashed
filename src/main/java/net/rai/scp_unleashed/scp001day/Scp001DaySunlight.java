package net.rai.scp_unleashed.scp001day;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp001day.item.Scp001DayWrapItem;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayExposure;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayFarField;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySun;
import net.rai.scp_unleashed.scp001day.sun.SunVector;
import net.rai.scp_unleashed.scp001day.world.Scp001DayFar;

public final class Scp001DaySunlight {
    public static final int CheckTicks = 2;
    public static final int WearTicks = 40;
    public static final int TriggerTicks = 8;
    public static final double LitPoint = 0.5;
    public static final EquipmentSlot[] Cover = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.CHEST};
    public static final EquipmentSlot[] Pieces = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<UUID, State> States = new HashMap<>();

    private Scp001DaySunlight() {
    }

    public static final class State {
        public float Exposure;
        public int Charge;
        public int Lit;
        public final double[] Points = new double[Scp001DayExposure.Samples];
    }

    public static State state(LivingEntity Body) {
        return States.computeIfAbsent(Body.getUUID(), Key -> new State());
    }

    public static void forget(LivingEntity Body) {
        States.remove(Body.getUUID());
    }

    public static void clear() {
        States.clear();
    }

    public static Scp001DayFarField far(ServerLevel World) {
        return Scp001DayWorld.strict(World) ? Scp001DayFar.City : Scp001DayFarField.Open;
    }

    public static SunVector sun(ServerLevel World) {
        return Scp001DaySun.direction(World.getDayTime());
    }

    public static float exposure(ServerLevel World, LivingEntity Body, State Into) {
        return exposure(World, Body, Into, sun(World));
    }

    public static float exposure(ServerLevel World, LivingEntity Body, State Into, SunVector Sun) {
        if (!Scp001DaySun.lethal(Sun)) {
            java.util.Arrays.fill(Into.Points, 0.0);
            Into.Lit = 0;
            return 0.0f;
        }
        Scp001DayExposure.samples(World, far(World), Body, Sun, Into.Points);
        double Strength = Scp001DaySun.intensity(Sun);
        double Total = 0.0;
        int Lit = 0;
        for (int Index = 0; Index < Into.Points.length; Index++) {
            if (Scp001DayWrapItem.shields(Body, Cover[Index])) {
                continue;
            }
            Total += Into.Points[Index];
            if (Into.Points[Index] * Strength >= LitPoint) {
                Lit++;
            }
        }
        Into.Lit = Lit;
        return (float) (Strength * Total / Into.Points.length);
    }

    public static void tick(ServerLevel World, ServerPlayer User) {
        tick(World, User, World.getGameTime());
    }

    public static void tick(ServerLevel World, ServerPlayer User, long Now) {
        if (User.isSpectator() || User.getAbilities().invulnerable || !User.isAlive()) {
            forget(User);
            return;
        }
        State Here = state(User);
        if (Now % CheckTicks != 0) {
            return;
        }
        Here.Exposure = exposure(World, User, Here);
        if (Now % WearTicks == 0) {
            wear(World, User, Here);
        }
        if (Scp001DayTurning.turning(User)) {
            Here.Charge = 0;
            return;
        }
        Here.Charge = Here.Lit > 0 ? Here.Charge + CheckTicks : Math.max(0, Here.Charge - CheckTicks);
        if (Here.Lit > 0) {
            World.sendParticles(ParticleTypes.SMOKE, User.getX(), User.getY() + User.getBbHeight() * 0.6, User.getZ(), 2, 0.25, 0.4, 0.25, 0.01);
            if (Now % 6 == 0) {
                World.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Scp001DayRegistry.Burn.get(), SoundSource.PLAYERS, 0.6f, 0.9f + 0.3f * World.random.nextFloat());
            }
        }
        if (Here.Charge >= TriggerTicks) {
            Here.Charge = 0;
            Scp001DayTurning.begin(World, User);
        }
    }

    public static void wear(ServerLevel World, ServerPlayer User, State Here) {
        SunVector Sun = sun(World);
        double Strength = Scp001DaySun.intensity(Sun);
        if (Strength <= 0.0) {
            return;
        }
        for (EquipmentSlot Slot : Pieces) {
            double Lit = 0.0;
            for (int Index = 0; Index < Cover.length; Index++) {
                if (Cover[Index] == Slot) {
                    Lit = Math.max(Lit, Here.Points[Index] * Strength);
                }
            }
            if (Lit > 0.0 && (Lit >= 0.5 || World.random.nextDouble() < Lit * 2.0)) {
                Scp001DayWrapItem.wear(World, User, Slot, 1);
            }
        }
    }
}
