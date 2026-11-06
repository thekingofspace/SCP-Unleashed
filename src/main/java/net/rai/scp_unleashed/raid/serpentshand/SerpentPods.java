package net.rai.scp_unleashed.raid.serpentshand;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.raid.hostile.HostileTrophy;
import net.rai.scp_unleashed.stasis.StasisContents;
import net.rai.scp_unleashed.stasis.StasisEffects;
import net.rai.scp_unleashed.stasis.StasisPods;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class SerpentPods {
    public static final FragmentRarity Grade = FragmentRarity.Wavering;
    public static final DustParticleOptions Teal = HostileRaider.dust(0.18f, 0.82f, 0.72f, 1.1f);
    public static final DustParticleOptions Gold = HostileRaider.dust(0.95f, 0.74f, 0.24f, 1.0f);

    private SerpentPods() {
    }

    @Nullable
    public static HostileTrophy capture(ServerLevel World, Mob Target, @Nullable Entity Holder) {
        if (Target.isLeashed()) {
            Target.dropLeash(true, false);
        }
        Target.ejectPassengers();
        Target.stopRiding();
        HostileTrophy Trophy = HostileTrophy.of(Target);
        if (Trophy == null) {
            StasisEffects.deny(World, Target.getBoundingBox().getCenter(), Grade);
            return null;
        }
        StasisEffects.capture(World, Target, Holder, Grade);
        burst(World, Target.getBoundingBox().getCenter(), Target.getBbWidth(), Target.getBbHeight());
        World.gameEvent(Holder, GameEvent.ENTITY_INTERACT, Target.position());
        Target.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        return Trophy;
    }

    public static void beam(ServerLevel World, Vec3 From, Vec3 To, int Step) {
        Vec3 Span = To.subtract(From);
        int Points = Math.max(4, (int) (Span.length() * 4.0));
        for (int Index = 0; Index <= Points; Index++) {
            if ((Index + Step) % 2 != 0) {
                continue;
            }
            Vec3 At = From.add(Span.scale(Index / (double) Points));
            World.sendParticles(Index % 4 == 0 ? Gold : Teal, At.x, At.y, At.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    public static void burst(ServerLevel World, Vec3 Centre, float Width, float Height) {
        World.sendParticles(Teal, Centre.x, Centre.y, Centre.z, 30, Width * 0.5, Height * 0.5, Width * 0.5, 0.0);
        World.sendParticles(Gold, Centre.x, Centre.y, Centre.z, 14, Width * 0.4, Height * 0.4, Width * 0.4, 0.0);
        World.sendParticles(ParticleTypes.END_ROD, Centre.x, Centre.y, Centre.z, 6, Width * 0.3, Height * 0.3, Width * 0.3, 0.02);
    }

    public static boolean release(ServerLevel World, StasisContents Held, Vec3 Where, float Yaw, @Nullable Player User) {
        Optional<Entity> Made = StasisPods.create(World, Held);
        if (Made.isEmpty()) {
            tell(User, Component.translatable(SerpentPodItem.Key + "failed", Held.Label()));
            StasisEffects.deny(World, Where, Grade);
            return false;
        }
        Entity Spawned = Made.get();
        Vec3 Spot = StasisPods.findRoom(World, Spawned, Where, Yaw);
        Spawned.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        if (!World.noCollision(Spawned)) {
            tell(User, Component.translatable(SerpentPodItem.Key + "no_room", Held.Label()));
            StasisEffects.deny(World, Where, Grade);
            return false;
        }
        if (Spawned instanceof LivingEntity Living && Held.Health() > 0.0f) {
            Living.setHealth(Math.min(Living.getMaxHealth(), Held.Health()));
        }
        if (!StasisPods.place(World, Spawned, Yaw)) {
            tell(User, Component.translatable(SerpentPodItem.Key + "failed", Held.Label()));
            StasisEffects.deny(World, Where, Grade);
            return false;
        }
        StasisEffects.release(World, Spawned, Grade);
        burst(World, Spawned.getBoundingBox().getCenter(), Spawned.getBbWidth(), Spawned.getBbHeight());
        World.gameEvent(User, GameEvent.ENTITY_PLACE, Spawned.position());
        tell(User, Component.translatable(SerpentPodItem.Key + "released", Held.Label()));
        return true;
    }

    private static void tell(@Nullable Player User, Component Text) {
        if (User != null) {
            User.displayClientMessage(Text, true);
        }
    }
}
