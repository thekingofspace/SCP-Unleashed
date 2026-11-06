package net.rai.scp_unleashed.scp008;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp008Events {
    public static final int ZombieCap = 16;

    private Scp008Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp008Registry.ZombieType.get(), Scp008Zombie.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp008Registry.ZombieEgg.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent Event) {
        LivingEntity Fallen = Event.getEntity();
        if (!(Fallen.level() instanceof ServerLevel World) || !(Fallen instanceof ServerPlayer || Fallen instanceof AbstractVillager || ControlBodies.isPersonLike(Fallen))) {
            return;
        }
        boolean Carrying = Scp008Infection.isInfected(Fallen);
        if (!Carrying && !(Event.getSource().getEntity() instanceof Scp008Zombie)) {
            return;
        }
        Scp008Infection.cure(Fallen);
        if (countRisen(World) >= ZombieCap) {
            return;
        }
        Scp008Zombie Risen = Scp008Registry.ZombieType.get().create(World);
        if (Risen == null) {
            return;
        }
        Risen.moveTo(Fallen.getX(), Fallen.getY(), Fallen.getZ(), Fallen.getYRot(), 0.0f);
        Risen.finalizeSpawn(World, World.getCurrentDifficultyAt(Fallen.blockPosition()), MobSpawnType.EVENT, null);
        if (Fallen instanceof ServerPlayer FallenPlayer) {
            Risen.adoptProfile(FallenPlayer);
        } else if (Fallen instanceof AbstractVillager FallenVillager) {
            Risen.adoptVillager(FallenVillager);
        } else if (ControlBodies.isPersonLike(Fallen)) {
            Risen.adoptBody(Fallen);
        }
        Risen.setPersistenceRequired();
        if (!World.addFreshEntity(Risen)) {
            return;
        }
        play(World, Risen, Scp008Registry.Risen);
        World.sendParticles(ParticleTypes.ASH, Risen.getX(), Risen.getY() + 1.0, Risen.getZ(), 24, 0.35, 0.6, 0.35, 0.01);
    }

    public static void collapse(Scp008Zombie Risen) {
        if (!(Risen.level() instanceof ServerLevel World)) {
            return;
        }
        play(World, Risen, Scp008Registry.Collapsed);
        World.sendParticles(ParticleTypes.ASH, Risen.getX(), Risen.getY() + 1.0, Risen.getZ(), 40, 0.4, 0.7, 0.4, 0.02);
        Risen.discard();
    }

    private static void play(ServerLevel World, Entity Source, List<DeferredHolder<SoundEvent, SoundEvent>> Layers) {
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Source.getX(), Source.getY(), Source.getZ(), Layer.get(), SoundSource.HOSTILE, 1.0f, Pitch);
        }
    }

    public static int countRisen(ServerLevel World) {
        return World.getEntities(Scp008Registry.ZombieType.get(), Found -> Found.isAlive()).size();
    }
}
