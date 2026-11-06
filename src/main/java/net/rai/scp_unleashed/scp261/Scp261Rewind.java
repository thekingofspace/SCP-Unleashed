package net.rai.scp_unleashed.scp261;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Scp261Rewind {
    public static final double MaxDistance = 256.0;

    private Scp261Rewind() {
    }

    public static void anchor(ServerPlayer User) {
        CompoundTag Mark = new CompoundTag();
        Mark.putString("Dimension", User.level().dimension().location().toString());
        Mark.putDouble("X", User.getX());
        Mark.putDouble("Y", User.getY());
        Mark.putDouble("Z", User.getZ());
        User.setData(Scp261Registry.RewindAnchor, Mark);
    }

    public static void clear(ServerPlayer User) {
        User.removeData(Scp261Registry.RewindAnchor);
    }

    public static boolean hasAnchor(ServerPlayer User) {
        return User.hasData(Scp261Registry.RewindAnchor) && !User.getData(Scp261Registry.RewindAnchor).isEmpty();
    }

    @Nullable
    private static Vec3 spotFor(ServerPlayer User) {
        if (!hasAnchor(User)) {
            return null;
        }
        CompoundTag Mark = User.getData(Scp261Registry.RewindAnchor);
        if (!User.level().dimension().location().toString().equals(Mark.getString("Dimension"))) {
            return null;
        }
        return new Vec3(Mark.getDouble("X"), Mark.getDouble("Y"), Mark.getDouble("Z"));
    }

    public static void trail(ServerLevel World, ServerPlayer User) {
        Vec3 Spot = spotFor(User);
        if (Spot != null) {
            World.sendParticles(ParticleTypes.REVERSE_PORTAL, Spot.x, Spot.y + 0.9, Spot.z, 6, 0.25, 0.5, 0.25, 0.01);
        }
    }

    public static boolean snapBack(ServerPlayer User) {
        Vec3 Spot = spotFor(User);
        clear(User);
        if (Spot == null || !User.isAlive() || Spot.distanceToSqr(User.position()) > MaxDistance * MaxDistance) {
            return false;
        }
        ServerLevel World = User.serverLevel();
        AABB Room = User.getDimensions(User.getPose()).makeBoundingBox(Spot).deflate(1.0e-3);
        if (!World.noCollision(User, Room) || !World.isLoaded(BlockPos.containing(Spot))) {
            return false;
        }
        Vec3 Left = User.position();
        if (User.isPassenger()) {
            User.stopRiding();
        }
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, User.getYRot(), User.getXRot());
        User.resetFallDistance();
        User.setDeltaMovement(Vec3.ZERO);
        User.hurtMarked = true;
        World.sendParticles(ParticleTypes.REVERSE_PORTAL, Left.x, Left.y + 0.9, Left.z, 24, 0.3, 0.6, 0.3, 0.05);
        World.sendParticles(ParticleTypes.PORTAL, Spot.x, Spot.y + 0.9, Spot.z, 24, 0.3, 0.6, 0.3, 0.3);
        Scp261Registry.playCue(World, Left, Scp261Registry.RewindCue, SoundSource.PLAYERS, 0.7f, 1.0f);
        Scp261Registry.playCue(World, Spot, Scp261Registry.RewindCue, SoundSource.PLAYERS, 0.7f, 1.2f);
        return true;
    }
}
