package net.rai.scp_unleashed.scp500;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassFood;
import net.rai.scp_unleashed.scp006.Scp006Mending;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public final class Scp500Panacea {
    public static final int Sparkles = 10;
    public static final int Motes = 6;
    public static final float MoteSize = 0.7f;
    public static final Vector3f MoteColour = new Vector3f(0.93f, 0.17f, 0.21f);
    public static final float SwallowVolume = 0.8f;
    public static final float FeedModifier = 1.0f;

    private Scp500Panacea() {
    }

    public static boolean canDose(LivingEntity Target) {
        return Target.isAlive() && !Target.isDeadOrDying() && !Target.isSpectator() && !(Target instanceof ArmorStand);
    }

    public static boolean dose(LivingEntity Target, @Nullable Entity Source) {
        if (!(Target.level() instanceof ServerLevel World) || !canDose(Target)) {
            return false;
        }
        Scp006Mending.cure(Target, Source);
        clearEffects(Target);
        Target.clearFire();
        Target.setTicksFrozen(0);
        restore(Target);
        feed(Target);
        feedback(World, Target);
        return true;
    }

    public static void clearEffects(LivingEntity Target) {
        for (MobEffectInstance Instance : List.copyOf(Target.getActiveEffects())) {
            Target.removeEffect(Instance.getEffect());
        }
    }

    public static void restore(LivingEntity Target) {
        if (!Target.getType().is(Tags.EntityTypes.BOSSES)) {
            Target.setHealth(Target.getMaxHealth());
        }
    }

    public static void feed(LivingEntity Target) {
        if (Target instanceof Player User) {
            FoodData Food = User.getFoodData();
            Food.setFoodLevel(FoodConstants.MAX_FOOD);
            Food.setSaturation(FoodConstants.MAX_SATURATION);
            Food.setExhaustion(0.0f);
        } else if (Target instanceof DClass Subject) {
            Subject.food().eat(DClassFood.MaxFood, FeedModifier);
        }
    }

    private static void feedback(ServerLevel World, LivingEntity Target) {
        double Width = Target.getBbWidth() * 0.45;
        double Height = Target.getBbHeight() * 0.35;
        Vec3 Middle = Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0);
        World.sendParticles(ParticleTypes.INSTANT_EFFECT, Middle.x, Middle.y, Middle.z, Sparkles, Width, Height, Width, 0.0);
        World.sendParticles(new DustParticleOptions(MoteColour, MoteSize), Middle.x, Middle.y, Middle.z, Motes, Width, Height, Width, 0.0);
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        Scp500Registry.playLayers(World, Target.getEyePosition(), Scp500Registry.SwallowLayers, Target.getSoundSource(), SwallowVolume, Pitch);
    }
}
