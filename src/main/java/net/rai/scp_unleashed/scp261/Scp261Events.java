package net.rai.scp_unleashed.scp261;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.neoforged.neoforge.registries.DeferredItem;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp261Events {
    private Scp261Events() {
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            for (DeferredItem<Scp261SnackItem> Treat : Scp261Registry.snacks()) {
                Event.accept(Treat.get());
            }
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp261Registry.VendingItem.get());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp261Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void soundAtPosition(PlayLevelSoundEvent.AtPosition Event) {
        Scp261Echo.heard(Event.getLevel(), Event.getSound(), Event.getSource(), Event.getPosition().x, Event.getPosition().y, Event.getPosition().z,
                Event.getNewVolume(), Event.getNewPitch());
    }

    @SubscribeEvent
    public static void soundAtEntity(PlayLevelSoundEvent.AtEntity Event) {
        Entity Source = Event.getEntity();
        if (Source != null) {
            Scp261Echo.heard(Event.getLevel(), Event.getSound(), Event.getSource(), Source.getX(), Source.getY() + Source.getBbHeight() * 0.5, Source.getZ(),
                    Event.getNewVolume(), Event.getNewPitch());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp261Echo.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Pre Event) {
        Scp261Pals.tick(Event.getEntity());
    }

    @SubscribeEvent
    public static void jumped(LivingEvent.LivingJumpEvent Event) {
        Scp261Effects.jumped(Event.getEntity());
    }

    @SubscribeEvent
    public static void fell(LivingFallEvent Event) {
        float Multiplier = Scp261Effects.landed(Event.getEntity(), Event.getDistance());
        if (Multiplier != 1.0f) {
            Event.setDamageMultiplier(Event.getDamageMultiplier() * Multiplier);
        }
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post Event) {
        LivingEntity Victim = Event.getEntity();
        if (!(Victim.level() instanceof ServerLevel World)) {
            return;
        }
        DamageSource Source = Event.getSource();
        if (!(Source.getEntity() instanceof Player Attacker) || Attacker == Victim) {
            return;
        }
        if (Attacker.hasEffect(Scp261Registry.Dreadful) && Scp261Dread.frightens(Victim)) {
            Attacker.removeEffect(Scp261Registry.Dreadful);
        }
        if (Source.is(DamageTypes.PLAYER_ATTACK) && Source.getDirectEntity() == Attacker && Event.getNewDamage() > 0.0f) {
            Scp261Effects.struck(World, Attacker, Victim);
        }
    }

    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent Event) {
        LivingEntity Next = Event.getNewAboutToBeSetTarget();
        if (Next != null && Scp261Dread.dreads(Event.getEntity(), Next)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent Event) {
        LivingEntity Dead = Event.getEntity();
        if (Scp261Pals.isPal(Dead)) {
            Event.setCanceled(true);
            return;
        }
        if (Dead.level() instanceof ServerLevel World && !(Dead instanceof Player) && Event.getSource().getEntity() instanceof Player Killer) {
            Scp261Effects.fortune(World, Killer, Event.getDrops());
        }
    }

    @SubscribeEvent
    public static void experience(LivingExperienceDropEvent Event) {
        if (Scp261Pals.isPal(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void baby(BabyEntitySpawnEvent Event) {
        if (Scp261Pals.isPal(Event.getParentA()) || Scp261Pals.isPal(Event.getParentB())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void effectExpired(MobEffectEvent.Expired Event) {
        if (Event.getEffectInstance() == null) {
            return;
        }
        Holder<MobEffect> Effect = Event.getEffectInstance().getEffect();
        LivingEntity Target = Event.getEntity();
        if (Effect.is(Scp261Registry.Pocket.getKey())) {
            if (!Scp261Effects.roomToGrow(Target)) {
                Event.setCanceled(true);
            } else {
                Scp261Effects.grew(Target);
            }
            return;
        }
        if (!(Target instanceof ServerPlayer User)) {
            return;
        }
        if (Effect.is(Scp261Registry.Hollow.getKey())) {
            Scp261Effects.hollowEnds(User);
        } else if (Effect.is(Scp261Registry.Borrowed.getKey())) {
            Scp261Effects.tomorrowArrives(User);
        } else if (Effect.is(Scp261Registry.Rewinding.getKey())) {
            Scp261Rewind.snapBack(User);
        }
    }

    @SubscribeEvent
    public static void effectRemoved(MobEffectEvent.Remove Event) {
        Holder<MobEffect> Effect = Event.getEffect();
        LivingEntity Target = Event.getEntity();
        if (Effect.is(Scp261Registry.Pocket.getKey()) && Target.isAlive() && Target.hasEffect(Scp261Registry.Pocket) && !Scp261Effects.roomToGrow(Target)) {
            Event.setCanceled(true);
            return;
        }
        if (Effect.is(Scp261Registry.Rewinding.getKey()) && Target instanceof ServerPlayer User) {
            Scp261Rewind.clear(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && User.hasEffect(Scp261Registry.Rewinding)) {
            User.removeEffect(Scp261Registry.Rewinding);
            Scp261Rewind.clear(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp261Pals.ownerLeft(User.server, User.getUUID());
            Scp261Echo.forget(User.getUUID());
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp261Echo.forgetAll();
        Scp261Pals.forgetAll();
    }
}
