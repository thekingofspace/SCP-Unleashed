package net.rai.scp_unleashed.scp058;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.Investigations;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class Scp058Emergence {
    public static final int BurstMotes = 40;
    public static final int BurstDust = 30;

    private Scp058Emergence() {
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Pos = Found.Pos();
        Vec3 Spot = Vec3.atBottomCenterOf(Pos);
        UUID Carcass = null;
        Investigations Data = Investigations.get(World.getServer());
        Optional<Investigation> Case = Data.find(Found.Investigator().getUUID(), Found.DocumentId());
        if (Case.isPresent() && Scp058CowCondition.isMarked(Case.get())) {
            CompoundTag Cow = Case.get().getState().getCompound(Scp058CowCondition.CowKey);
            Vec3 Exact = Scp058CowCondition.spot(Cow);
            if (BlockPos.containing(Exact).equals(Pos)) {
                Spot = Exact;
            }
            Carcass = Scp058CowCondition.carcass(Cow);
            Scp058CowCondition.forget(Case.get());
            Data.setDirty();
        }
        Vec3 Toward = Found.Investigator().position().subtract(Spot);
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        Optional<Scp058Heart> Risen = emerge(World, Spot, Yaw, Found.Investigator().getUUID());
        if (Risen.isPresent() && Carcass != null && World.getEntity(Carcass) instanceof Cow Body && !Body.isAlive()) {
            Body.discard();
        }
        return Risen.isPresent();
    }

    public static Optional<Scp058Heart> emerge(ServerLevel World, Vec3 Spot, float Yaw, @Nullable UUID Witness) {
        Scp058Heart Heart = Scp058Registry.Heart.get().create(World);
        if (Heart == null) {
            return Optional.empty();
        }
        Heart.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Heart.setYHeadRot(Yaw);
        Heart.setYBodyRot(Yaw);
        if (!World.noCollision(Heart)) {
            return Optional.empty();
        }
        Heart.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        Heart.beginEmerging(Witness);
        if (!World.addFreshEntity(Heart)) {
            return Optional.empty();
        }
        burst(World, Spot);
        return Optional.of(Heart);
    }

    public static void burst(ServerLevel World, Vec3 Spot) {
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp058Registry.EmergeLayers) {
            World.playSound(null, Spot.x, Spot.y, Spot.z, Layer.get(), SoundSource.HOSTILE, 1.4f, Pitch);
        }
        World.sendParticles(Scp058Registry.Mote.get(), Spot.x, Spot.y + 0.4, Spot.z, BurstMotes, 0.45, 0.3, 0.45, 0.14);
        World.sendParticles(Scp058Heart.Ichor, Spot.x, Spot.y + 0.5, Spot.z, BurstDust, 0.6, 0.35, 0.6, 0.0);
    }

    public static void omen(ServerLevel World, Entity Carcass) {
        World.playSound(null, Carcass.getX(), Carcass.getY(), Carcass.getZ(), Scp058Registry.HeartbeatLow.get(), SoundSource.HOSTILE, 1.2f, 0.7f);
        World.playSound(null, Carcass.getX(), Carcass.getY(), Carcass.getZ(), Scp058Registry.Heartbeat.get(), SoundSource.HOSTILE, 1.0f, 0.8f);
        World.sendParticles(Scp058Registry.Mote.get(), Carcass.getX(), Carcass.getY() + 0.5, Carcass.getZ(), 8, 0.3, 0.2, 0.3, 0.04);
    }
}
