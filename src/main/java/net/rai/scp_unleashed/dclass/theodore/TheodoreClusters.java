package net.rai.scp_unleashed.dclass.theodore;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public final class TheodoreClusters {
    public static final int WorldCap = 64;
    public static final int AmalgamationCap = 6;
    public static final double ClusterReach = 48.0;

    private TheodoreClusters() {
    }

    public static List<Theodore> members(ServerLevel World, Theodore Eater) {
        UUID Id = Eater.cluster();
        AABB Area = Eater.getBoundingBox().inflate(ClusterReach, ClusterReach / 2.0, ClusterReach);
        return World.getEntitiesOfClass(Theodore.class, Area, Member -> Member.isAlive() && Id != null && Id.equals(Member.cluster()));
    }

    public static int population(ServerLevel World) {
        return World.getEntities(TheodoreRegistry.TheodoreType.get(), Entity::isAlive).size();
    }

    public static int masses(ServerLevel World) {
        return World.getEntities(TheodoreRegistry.AmalgamationType.get(), Entity::isAlive).size();
    }

    public static boolean canMerge(ServerLevel World) {
        return masses(World) < AmalgamationCap;
    }
}
