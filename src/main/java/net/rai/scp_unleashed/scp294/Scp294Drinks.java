package net.rai.scp_unleashed.scp294;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.EffectCures;

import java.util.List;
import java.util.Optional;

public final class Scp294Drinks {
    public static final int TeleportTries = 16;
    public static final double ParticleSpread = 0.4;

    private Scp294Drinks() {
    }

    public static void apply(LivingEntity Drinker, Scp294Drink Drink) {
        if (!(Drinker.level() instanceof ServerLevel World)) {
            return;
        }
        if (!Drink.Message().isBlank() && Drinker instanceof Player User) {
            User.displayClientMessage(Drink.messageText().copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        for (Scp294Action Action : Drink.Actions()) {
            if (!Drinker.isAlive()) {
                return;
            }
            act(World, Drinker, Action);
        }
        for (Scp294Dose Dose : Drink.Effects()) {
            if (!Drinker.isAlive()) {
                return;
            }
            dose(Drinker, Dose);
        }
    }

    public static void dose(LivingEntity Drinker, Scp294Dose Dose) {
        Optional<Holder.Reference<MobEffect>> Found = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(Registries.MOB_EFFECT, Dose.Effect()));
        if (Found.isEmpty()) {
            return;
        }
        Holder<MobEffect> Effect = Found.get();
        if (Effect.value().isInstantenous()) {
            Effect.value().applyInstantenousEffect(null, null, Drinker, Dose.Amplifier(), 1.0);
        } else {
            Drinker.addEffect(new MobEffectInstance(Effect, Dose.Ticks(), Dose.Amplifier()));
        }
    }

    public static void act(ServerLevel World, LivingEntity Drinker, Scp294Action Action) {
        switch (Action.Type()) {
            case Scp294Action.Heal -> Drinker.heal(Action.Amount());
            case Scp294Action.Damage -> Drinker.hurt(Scp294DamageSource.of(World, Scp294Registry.DrinkDamage, Action.Text()), Action.Amount());
            case Scp294Action.Ignite -> Drinker.igniteForSeconds(Action.Amount());
            case Scp294Action.Levitate -> Drinker.addEffect(new MobEffectInstance(MobEffects.LEVITATION, Math.max(1, Math.round(Action.Amount() * 20)), (int) Action.Extra()));
            case Scp294Action.TeleportRandom -> teleport(World, Drinker, Action.Amount());
            case Scp294Action.ClearEffects -> Drinker.removeEffectsCuredBy(EffectCures.MILK);
            case Scp294Action.Feed -> {
                if (Drinker instanceof Player User) {
                    User.getFoodData().eat(new FoodProperties(Math.round(Action.Amount()), Action.Extra(), true, 1.6f, Optional.empty(), List.of()));
                }
            }
            case Scp294Action.Extinguish -> {
                Drinker.clearFire();
                Drinker.setTicksFrozen(0);
            }
            case Scp294Action.Glow -> Drinker.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.max(1, Math.round(Action.Amount() * 20)), 0));
            case Scp294Action.Explode -> World.explode(null, Drinker.getX(), Drinker.getY() + 0.1, Drinker.getZ(), Mth.clamp(Action.Amount(), 0.5f, 4.0f), Level.ExplosionInteraction.NONE);
            case Scp294Action.Vanish -> Scp294Ascension.begin(Drinker, Math.round(Action.Amount() * 20));
            case Scp294Action.Arsenic -> World.sendParticles(Scp294Ascension.Sickly, Drinker.getX(), Drinker.getEyeY() - 0.3, Drinker.getZ(), 10, 0.25, 0.2, 0.25, 0.0);
            case Scp294Action.Sound -> sound(World, Drinker, Action);
            case Scp294Action.Particles -> particles(World, Drinker, Action);
            default -> {
            }
        }
    }

    private static void sound(ServerLevel World, LivingEntity Drinker, Scp294Action Action) {
        ResourceLocation Id = ResourceLocation.tryParse(Action.Text());
        if (Id == null) {
            return;
        }
        SoundEvent Sound = BuiltInRegistries.SOUND_EVENT.getOptional(Id).orElseGet(() -> SoundEvent.createVariableRangeEvent(Id));
        World.playSound(null, Drinker.getX(), Drinker.getEyeY(), Drinker.getZ(), Sound, SoundSource.PLAYERS, Action.Amount(), Action.Extra() <= 0.0f ? 1.0f : Action.Extra());
    }

    private static void particles(ServerLevel World, LivingEntity Drinker, Scp294Action Action) {
        ResourceLocation Id = ResourceLocation.tryParse(Action.Text());
        ParticleType<?> Kind = Id == null ? null : BuiltInRegistries.PARTICLE_TYPE.get(Id);
        if (Kind instanceof SimpleParticleType Simple) {
            World.sendParticles(Simple, Drinker.getX(), Drinker.getY() + Drinker.getBbHeight() * 0.6, Drinker.getZ(), Math.max(1, Math.round(Action.Amount())),
                    ParticleSpread, Drinker.getBbHeight() * 0.35, ParticleSpread, 0.04);
        }
    }

    private static void teleport(ServerLevel World, LivingEntity Drinker, float Radius) {
        RandomSource Random = Drinker.getRandom();
        Vec3 Start = Drinker.position();
        for (int Attempt = 0; Attempt < TeleportTries; Attempt++) {
            double X = Drinker.getX() + (Random.nextDouble() - 0.5) * 2.0 * Radius;
            double Y = Mth.clamp(Drinker.getY() + (Random.nextInt(Math.max(2, Math.round(Radius))) - Radius / 2.0), World.getMinBuildHeight(),
                    World.getMinBuildHeight() + World.getLogicalHeight() - 1);
            double Z = Drinker.getZ() + (Random.nextDouble() - 0.5) * 2.0 * Radius;
            if (Drinker.isPassenger()) {
                Drinker.stopRiding();
            }
            if (Drinker.randomTeleport(X, Y, Z, true)) {
                World.gameEvent(GameEvent.TELEPORT, Start, GameEvent.Context.of(Drinker));
                World.playSound(null, Drinker.getX(), Drinker.getY(), Drinker.getZ(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                Drinker.resetFallDistance();
                return;
            }
        }
    }
}
