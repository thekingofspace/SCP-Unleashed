package net.rai.scp_unleashed.scp939;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Scp939Population {
    private Scp939Population() {
    }

    public static int room(ServerLevel World, Vec3 Where) {
        AABB Area = new AABB(Where, Where).inflate(Scp939.AreaRadius);
        int Adults = World.getEntitiesOfClass(Scp939.class, Area, Entity::isAlive).size();
        int Broods = World.getEntitiesOfClass(Villager.class, Area, Each -> Each.isAlive() && Scp939Brood.isBrood(Each)).size();
        return Adults + Broods;
    }

    public static int level(ServerLevel World) {
        int Count = World.getEntities(Scp939Registry.Scp939Type.get(), Entity::isAlive).size();
        Count += World.getEntities(EntityType.VILLAGER, Each -> Each.isAlive() && Scp939Brood.isBrood(Each)).size();
        return Count;
    }
}
