package net.rai.scp_unleashed.scp024;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp024Relocation {
    public static final TagKey<EntityType<?>> PersonLike = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "person_like"));
    public static final int SweepEvery = 40;
    private static final Set<UUID> Pending = ConcurrentHashMap.newKeySet();

    private Scp024Relocation() {
    }

    public static boolean isPersonLike(Entity Target) {
        return Target.getType().is(PersonLike);
    }

    public static boolean belongsOutside(Entity Target) {
        if (Target instanceof Player || Scp024Stage.isOwned(Target) || Target.isRemoved()) {
            return false;
        }
        if (Target instanceof LivingEntity Living && !Living.isAlive()) {
            return false;
        }
        return Target instanceof LivingEntity || isPersonLike(Target);
    }

    public static void queue(Entity Target) {
        Pending.add(Target.getUUID());
    }

    public record Spot(ServerLevel World, Vec3 Pos, float Yaw) {
    }

    public static Spot outside(MinecraftServer Server) {
        Scp024World Data = Scp024World.get(Server);
        BlockPos Door = Data.door();
        if (Door != null) {
            return new Spot(Server.overworld(), Scp024Runs.doorstep(Door, Data.front(), 2.5), Data.front().toYRot());
        }
        ServerLevel Home = Server.overworld();
        BlockPos Spawn = Home.getSharedSpawnPos();
        BlockPos Top = Home.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Spawn);
        return new Spot(Home, Vec3.atBottomCenterOf(Top), 0.0f);
    }

    public static void moveOut(MinecraftServer Server, Entity Target) {
        if (Target.isRemoved() || Target instanceof Player) {
            return;
        }
        Spot Where = outside(Server);
        Vec3 Jitter = new Vec3((Target.getRandom().nextDouble() - 0.5) * 1.5, 0.0, (Target.getRandom().nextDouble() - 0.5) * 1.5);
        Target.stopRiding();
        Target.ejectPassengers();
        Target.changeDimension(new DimensionTransition(Where.World(), Where.Pos().add(Jitter), Vec3.ZERO, Where.Yaw(), 0.0f, DimensionTransition.DO_NOTHING));
    }

    public static void tick(MinecraftServer Server) {
        ServerLevel Stage = Scp024Stage.level(Server);
        if (Stage == null) {
            Pending.clear();
            return;
        }
        List<Entity> Moving = new ArrayList<>();
        if (!Pending.isEmpty()) {
            for (UUID Id : new ArrayList<>(Pending)) {
                Pending.remove(Id);
                Entity Found = Stage.getEntity(Id);
                if (Found != null && belongsOutside(Found)) {
                    Moving.add(Found);
                }
            }
        }
        if (Server.getTickCount() % SweepEvery == 0) {
            for (Entity Found : Stage.getAllEntities()) {
                if (belongsOutside(Found) && !Moving.contains(Found)) {
                    Moving.add(Found);
                }
            }
        }
        for (Entity Found : Moving) {
            moveOut(Server, Found);
        }
    }
}
