package net.rai.scp_unleashed.scp457;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.control.ControlSession;
import net.rai.scp_unleashed.control.ControlSessions;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp457Spread {
    public static final int MaxPerDimension = 16;
    public static final int MarkTicks = 300;
    public static final int PruneSize = 512;
    public static final int RoomRadius = 3;
    private static final Map<UUID, Long> Marks = new ConcurrentHashMap<>();

    private Scp457Spread() {
    }

    public static void mark(ServerLevel World, LivingEntity Victim) {
        long Until = World.getGameTime() + MarkTicks;
        if (Marks.size() > PruneSize) {
            long Now = World.getGameTime();
            Marks.values().removeIf(Time -> Time < Now);
        }
        Marks.put(Victim.getUUID(), Until);
        if (Victim instanceof ServerPlayer User && ControlSessions.isControlling(User)) {
            ControlSession Session = ControlSessions.session(User);
            if (Session.active()) {
                Marks.put(Session.BodyId(), Until);
            }
        }
    }

    public static boolean marked(ServerLevel World, UUID Victim) {
        Long Until = Marks.get(Victim);
        return Until != null && Until >= World.getGameTime();
    }

    public static boolean credited(ServerLevel World, LivingEntity Victim, DamageSource Source) {
        if (Source.getEntity() instanceof Scp457 || Source.getDirectEntity() instanceof Scp457) {
            return true;
        }
        return Source.is(DamageTypeTags.IS_FIRE) && marked(World, Victim.getUUID());
    }

    public static void forget(UUID Victim) {
        Marks.remove(Victim);
    }

    public static void clear() {
        Marks.clear();
    }

    public static int count(ServerLevel World) {
        return World.getEntities(Scp457Registry.Burning.get(), Entity::isAlive).size();
    }

    public static boolean canSpread(ServerLevel World) {
        return Scp457Registry.spreads(World.getGameRules()) && count(World) < MaxPerDimension;
    }

    @Nullable
    public static Scp457 rise(ServerLevel World, Vec3 At, float Yaw) {
        if (!canSpread(World)) {
            return null;
        }
        Scp457 Made = Scp457Registry.Burning.get().create(World);
        if (Made == null) {
            return null;
        }
        Vec3 Spot = room(World, Made, At, Yaw);
        if (Spot == null) {
            Scp457Effects.fizzle(World, At);
            return null;
        }
        Made.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Made.setYBodyRot(Yaw);
        Made.setYHeadRot(Yaw);
        if (Scp457Water.touching(Made)) {
            Scp457Effects.fizzle(World, Spot);
            return null;
        }
        Made.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.MOB_SUMMONED, null);
        Made.setPrime(false);
        Made.beginRise();
        if (!World.addFreshEntity(Made)) {
            return null;
        }
        Scp457Effects.ignition(World, Made);
        return Made;
    }

    @Nullable
    public static Vec3 room(ServerLevel World, Scp457 Made, Vec3 At, float Yaw) {
        if (fits(World, Made, At)) {
            return At;
        }
        BlockPos Base = BlockPos.containing(At);
        for (int Radius = 1; Radius <= RoomRadius; Radius++) {
            for (int Dx = -Radius; Dx <= Radius; Dx++) {
                for (int Dz = -Radius; Dz <= Radius; Dz++) {
                    if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Radius) {
                        continue;
                    }
                    for (int Dy = 1; Dy >= -2; Dy--) {
                        BlockPos Pos = Base.offset(Dx, Dy, Dz);
                        if (!World.getBlockState(Pos.below()).isFaceSturdy(World, Pos.below(), Direction.UP)) {
                            continue;
                        }
                        Vec3 Spot = Vec3.atBottomCenterOf(Pos);
                        if (fits(World, Made, Spot)) {
                            return Spot;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean fits(ServerLevel World, Scp457 Made, Vec3 Spot) {
        AABB Box = Made.getDimensions(Made.getPose()).makeBoundingBox(Spot);
        return World.noCollision(Made, Box) && !World.containsAnyLiquid(Box) && !Scp457Water.inCauldron(World, Box);
    }
}
