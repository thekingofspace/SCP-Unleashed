package net.rai.scp_unleashed.scp335;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Scp335Bodies {
    public static final int Burst = 48;
    private static final List<Left> Waiting = new ArrayList<>();

    private Scp335Bodies() {
    }

    public static boolean claim(Entity Joining) {
        if (!(Joining instanceof LivingEntity Body) || Joining instanceof Player || !(Joining.level() instanceof ServerLevel World)) {
            return false;
        }
        if (Body.getExistingDataOrNull(Scp335Registry.Episode.get()) == null) {
            return false;
        }
        Body.removeData(Scp335Registry.Episode.get());
        Waiting.add(new Left(World.dimension(), Body.getUUID()));
        return true;
    }

    public static void tick(MinecraftServer Server) {
        if (Waiting.isEmpty()) {
            return;
        }
        List<Left> Due = List.copyOf(Waiting);
        Waiting.clear();
        for (Left Entry : Due) {
            ServerLevel World = Server.getLevel(Entry.Dimension());
            if (World != null && World.getEntity(Entry.Body()) instanceof LivingEntity Body && Body.isAlive()) {
                absorb(World, Body);
            }
        }
    }

    public static void absorb(ServerLevel World, LivingEntity Body) {
        Vec3 At = Body.position();
        World.sendParticles(Scp335Registry.Pixel.get(), At.x, At.y + Body.getBbHeight() * 0.5, At.z, Burst, Body.getBbWidth() * 0.4, Body.getBbHeight() * 0.35,
                Body.getBbWidth() * 0.4, 0.06);
        Scp335Episodes.playAt(World, At, Scp335Registry.DissolveLayers, 1.0f, 1.1f);
        Body.setInvisible(true);
        Body.invulnerableTime = 0;
        Body.hurt(World.damageSources().source(Scp335Registry.Absorbed), Float.MAX_VALUE);
        if (Body.isDeadOrDying() && !Body.isRemoved()) {
            Body.remove(Entity.RemovalReason.KILLED);
        } else if (Body.isAlive()) {
            Body.setInvisible(false);
        }
    }

    private record Left(ResourceKey<Level> Dimension, UUID Body) {
    }
}
