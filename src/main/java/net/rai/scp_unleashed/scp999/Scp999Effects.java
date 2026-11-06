package net.rai.scp_unleashed.scp999;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class Scp999Effects {
    public static final float GiggleVolume = 0.75f;
    public static final int Crumbs = 6;

    private Scp999Effects() {
    }

    public static void layers(ServerLevel World, Entity Owner, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        if (Owner.isSilent()) {
            return;
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Owner.getX(), Owner.getY() + Owner.getBbHeight() * 0.5, Owner.getZ(), Layer.get(), Owner.getSoundSource(), Volume, Pitch);
        }
    }

    public static void hearts(ServerLevel World, Entity Owner, int Count) {
        World.sendParticles(ParticleTypes.HEART, Owner.getX(), Owner.getY() + Owner.getBbHeight() + 0.2, Owner.getZ(), Count, Owner.getBbWidth() * 0.35, 0.1,
                Owner.getBbWidth() * 0.35, 0.02);
    }

    public static void sparkle(ServerLevel World, Entity Owner) {
        World.sendParticles(ParticleTypes.HAPPY_VILLAGER, Owner.getX(), Owner.getY() + Owner.getBbHeight() * 0.8, Owner.getZ(), 5, Owner.getBbWidth() * 0.4, 0.15,
                Owner.getBbWidth() * 0.4, 0.02);
    }

    public static void curious(ServerLevel World, Entity Owner) {
        World.sendParticles(ParticleTypes.NOTE, Owner.getX(), Owner.getY() + Owner.getBbHeight() + 0.3, Owner.getZ(), 1, 0.0, 0.0, 0.0, 0.3);
    }

    public static void notes(ServerLevel World, Entity Owner) {
        World.sendParticles(ParticleTypes.NOTE, Owner.getX(), Owner.getY() + Owner.getBbHeight() + 0.3, Owner.getZ(), 2, 0.2, 0.1, 0.2, 0.5);
    }

    public static void snooze(ServerLevel World, Entity Owner) {
        World.sendParticles(ParticleTypes.CLOUD, Owner.getX(), Owner.getY() + Owner.getBbHeight() + 0.15, Owner.getZ(), 1, 0.05, 0.02, 0.05, 0.0);
    }

    public static void fright(ServerLevel World, Entity Owner) {
        World.sendParticles(ParticleTypes.SPLASH, Owner.getX(), Owner.getY() + Owner.getBbHeight() * 0.6, Owner.getZ(), 6, 0.2, 0.05, 0.2, 0.1);
    }

    public static void tremble(ServerLevel World, Entity Owner) {
        World.sendParticles(ParticleTypes.FALLING_DRIPSTONE_WATER, Owner.getX(), Owner.getY() + 0.3, Owner.getZ(), 1, Owner.getBbWidth() * 0.4, 0.0, Owner.getBbWidth() * 0.4, 0.0);
    }

    public static void crumbs(ServerLevel World, Entity Owner) {
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Scp999Registry.Candy.get())), Owner.getX(), Owner.getY() + Owner.getBbHeight() * 0.5,
                Owner.getZ(), Crumbs, Owner.getBbWidth() * 0.3, 0.1, Owner.getBbWidth() * 0.3, 0.04);
        hearts(World, Owner, 2);
    }

    public static void giggle(ServerLevel World, Scp999Blob Blob, LivingEntity Hugged) {
        if (!Blob.isSilent()) {
            float Pitch = 0.95f + World.getRandom().nextFloat() * 0.12f;
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp999Registry.GiggleLayers) {
                World.playSound(null, Hugged.getX(), Hugged.getEyeY(), Hugged.getZ(), Layer.get(), Blob.getSoundSource(), GiggleVolume, Pitch);
            }
        }
        World.sendParticles(ParticleTypes.HEART, Hugged.getX(), Hugged.getEyeY() + 0.4, Hugged.getZ(), 1, 0.25, 0.1, 0.25, 0.02);
        World.sendParticles(ParticleTypes.NOTE, Hugged.getX(), Hugged.getEyeY() + 0.5, Hugged.getZ(), 1, 0.2, 0.1, 0.2, 0.6);
    }
}
