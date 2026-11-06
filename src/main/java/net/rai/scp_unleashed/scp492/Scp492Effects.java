package net.rai.scp_unleashed.scp492;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class Scp492Effects {
    public static final int StuffingPuff = 6;
    public static final int StuffingSpill = 18;
    public static final int SewThreads = 8;

    private Scp492Effects() {
    }

    public static void layers(ServerLevel World, Entity Owner, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        if (Owner.isSilent()) {
            return;
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Owner.getX(), Owner.getY() + Owner.getBbHeight() * 0.5, Owner.getZ(), Layer.get(), Owner.getSoundSource(), Volume, Pitch);
        }
    }

    public static void stuffing(ServerLevel World, Entity Owner, int Count) {
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()), Owner.getX(), Owner.getY() + Owner.getBbHeight() * 0.45,
                Owner.getZ(), Count, Owner.getBbWidth() * 0.4, Owner.getBbHeight() * 0.25, Owner.getBbWidth() * 0.4, 0.05);
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.WHEAT)), Owner.getX(), Owner.getY() + Owner.getBbHeight() * 0.45, Owner.getZ(),
                Math.max(1, Count / 3), Owner.getBbWidth() * 0.3, Owner.getBbHeight() * 0.2, Owner.getBbWidth() * 0.3, 0.06);
    }

    public static void threads(ServerLevel World, Entity Owner) {
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.STRING)), Owner.getX(), Owner.getY() + Owner.getBbHeight() * 0.5, Owner.getZ(),
                SewThreads, Owner.getBbWidth() * 0.35, Owner.getBbHeight() * 0.3, Owner.getBbWidth() * 0.35, 0.04);
    }

    public static void spotted(ServerLevel World, Entity Target) {
        World.sendParticles(ParticleTypes.NOTE, Target.getX(), Target.getY() + Target.getBbHeight() + 0.4, Target.getZ(), 1, 0.0, 0.0, 0.0, 0.6);
    }

    public static void fright(ServerLevel World, Entity Owner) {
        World.sendParticles(ParticleTypes.SPLASH, Owner.getX(), Owner.getY() + Owner.getBbHeight() * 0.9, Owner.getZ(), 6, 0.15, 0.08, 0.15, 0.1);
    }
}
