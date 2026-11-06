package net.rai.scp_unleashed.warhead;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.warhead.network.WarheadBlastPayload;

public final class WarheadBlast {
    private WarheadBlast() {
    }

    public static boolean isTarget(LivingEntity Target) {
        return Target.isAlive() && !Target.isSpectator() && !(Target instanceof Player User && User.isCreative()) && !(Target instanceof ArmorStand) && !Target.isInvulnerable();
    }

    public static DamageSource source(ServerLevel World, Vec3 Centre) {
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(WarheadRegistry.AlphaDamage), Centre);
    }

    public static int killZone(ServerLevel World, Vec3 Centre) {
        DamageSource Source = source(World, Centre);
        double Reach = WarheadNetwork.KillRange;
        int Killed = 0;
        for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, new AABB(Centre, Centre).inflate(Reach), WarheadBlast::isTarget)) {
            if (Target.distanceToSqr(Centre) > Reach * Reach) {
                continue;
            }
            Target.hurt(Source, Float.MAX_VALUE);
            if (!Target.isAlive()) {
                Killed++;
            }
        }
        return Killed;
    }

    public static void broadcast(ServerLevel World, Vec3 Centre) {
        WarheadBlastPayload Payload = new WarheadBlastPayload(Centre);
        double Range = WarheadNetwork.BlastNoticeRange;
        for (ServerPlayer Viewer : World.players()) {
            if (Viewer.distanceToSqr(Centre) <= Range * Range) {
                PacketDistributor.sendToPlayer(Viewer, Payload);
            }
        }
    }
}
