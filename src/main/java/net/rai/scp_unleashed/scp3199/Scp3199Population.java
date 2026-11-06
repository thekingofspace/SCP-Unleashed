package net.rai.scp_unleashed.scp3199;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Scp3199Population {
    private Scp3199Population() {
    }

    public static int creatures(ServerLevel World) {
        return World.getEntities(Scp3199Registry.Scp3199Type.get(), Entity::isAlive).size();
    }

    public static int eggs(ServerLevel World) {
        return World.getEntities(Scp3199Registry.EggType.get(), Entity::isAlive).size();
    }

    public static int level(ServerLevel World) {
        return creatures(World) + eggs(World);
    }

    public static int creatures(ServerLevel World, Vec3 Where, int Radius) {
        return World.getEntitiesOfClass(Scp3199.class, new AABB(Where, Where).inflate(Radius), Entity::isAlive).size();
    }

    public static int eggs(ServerLevel World, Vec3 Where, int Radius) {
        return World.getEntitiesOfClass(Scp3199Egg.class, new AABB(Where, Where).inflate(Radius), Entity::isAlive).size();
    }

    public static int area(ServerLevel World, Vec3 Where) {
        return creatures(World, Where, Scp3199Nesting.AreaRadius) + eggs(World, Where, Scp3199Nesting.AreaRadius);
    }
}
