package net.rai.scp_unleashed.scp458;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class Scp458Box {
    public static final int LidRest = 20;
    public static final float FrontAngle = 270.0f;
    public static final float OpenVolume = 0.85f;
    public static final float CloseVolume = 0.9f;
    public static final float TakeVolume = 0.75f;
    public static final float RestVolume = 0.5f;
    public static final double LidReachY = 4.5 / 16.0;
    public static final double LidReachZ = 0.5 / 16.0;

    public enum Outcome {
        Opened,
        Took,
        Closed,
        Resting,
        Refused
    }

    public record Step(Outcome Result, Scp458Contents After, ItemStack Given) {
        public boolean changed() {
            return Result == Outcome.Opened || Result == Outcome.Took || Result == Outcome.Closed;
        }
    }

    private Scp458Box() {
    }

    public static boolean canUse(LivingEntity User) {
        return !(User instanceof FakePlayer) && !User.isSpectator() && User.isAlive();
    }

    public static Step act(ServerLevel World, LivingEntity User, Scp458Contents Current, boolean Closing, float Angle) {
        if (!canUse(User)) {
            return new Step(Outcome.Refused, Current, ItemStack.EMPTY);
        }
        long Now = World.getGameTime();
        if (!Current.Open()) {
            if (Current.resting(Now)) {
                return new Step(Outcome.Resting, Current, ItemStack.EMPTY);
            }
            Scp458Pizza Served = Scp458Favourites.claim(User, World.getRandom());
            return new Step(Outcome.Opened, Current.opened(Served, Now), ItemStack.EMPTY);
        }
        if (Closing || Current.empty()) {
            return new Step(Outcome.Closed, Current.closed(Now), ItemStack.EMPTY);
        }
        int Chosen = Current.nearest(Angle);
        if (Chosen < 0 || Current.Pizza().isEmpty()) {
            return new Step(Outcome.Closed, Current.closed(Now), ItemStack.EMPTY);
        }
        return new Step(Outcome.Took, Current.without(Chosen), slice(Current.Pizza().get()));
    }

    public static ItemStack slice(Scp458Pizza Served) {
        ItemStack Made = new ItemStack(Scp458Registry.Slice.get());
        Made.set(Scp458Registry.Pizza.get(), Served);
        return Made;
    }

    public static void give(LivingEntity Taker, ItemStack Given) {
        if (Given.isEmpty()) {
            return;
        }
        if (Taker instanceof Player User) {
            if (!User.getInventory().add(Given) && !Given.isEmpty()) {
                User.drop(Given, false);
            }
            User.inventoryMenu.broadcastChanges();
        } else {
            Taker.spawnAtLocation(Given);
        }
    }

    public static void sound(ServerLevel World, Vec3 At, Outcome Result) {
        switch (Result) {
            case Opened -> play(World, At, Scp458Registry.OpenLayers, OpenVolume);
            case Closed -> play(World, At, Scp458Registry.CloseLayers, CloseVolume);
            case Took -> play(World, At, Scp458Registry.TakeLayers, TakeVolume);
            case Resting -> play(World, At, Scp458Registry.RestLayers, RestVolume);
            default -> {
            }
        }
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume) {
        float Pitch = 0.94f + World.getRandom().nextFloat() * 0.12f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    public static double[] local(Direction Facing, double X, double Z) {
        return switch (Facing) {
            case EAST -> new double[]{Z, -X};
            case SOUTH -> new double[]{-X, -Z};
            case WEST -> new double[]{-Z, X};
            default -> new double[]{X, Z};
        };
    }

    public static float angle(double LocalX, double LocalZ) {
        if (Math.abs(LocalX) < 1.0e-6 && Math.abs(LocalZ) < 1.0e-6) {
            return FrontAngle;
        }
        float Degrees = (float) Math.toDegrees(Math.atan2(LocalZ, LocalX));
        return (Degrees % 360.0f + 360.0f) % 360.0f;
    }

    public static float facingAngle(Direction Facing, Vec3 Centre, Vec3 Toward) {
        double[] Local = local(Facing, Toward.x - Centre.x, Toward.z - Centre.z);
        return angle(Local[0], Local[1]);
    }

    public static boolean onLid(Direction Facing, Vec3 Corner, Vec3 Hit) {
        double[] Local = local(Facing, Hit.x - Corner.x - 0.5, Hit.z - Corner.z - 0.5);
        return Hit.y - Corner.y > LidReachY && Local[1] + 0.5 > 1.0 - 3.0 / 16.0 - LidReachZ;
    }
}
