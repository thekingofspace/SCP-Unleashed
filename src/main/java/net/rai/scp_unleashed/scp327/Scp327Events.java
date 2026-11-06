package net.rai.scp_unleashed.scp327;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp327.client.Scp327ClientHooks;
import net.rai.scp_unleashed.scp327.network.Scp327HeldPayload;
import net.rai.scp_unleashed.scp327.network.Scp327LurePayload;
import net.rai.scp_unleashed.scp327.network.Scp327StrugglePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp327Events {
    public static final double SplashReach = 4.0;

    private Scp327Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp327Registry.Siren.get(), Scp327Siren.createAttributes(Scp327Siren.AdultHealth).build());
        Event.put(Scp327Registry.Pup.get(), Scp327Siren.createAttributes(Scp327Siren.BabyHealth).build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp327Registry.SirenEgg.get());
            Event.accept(Scp327Registry.PupEgg.get());
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp327LurePayload.PayloadType, Scp327LurePayload.PayloadCodec, (Payload, Context) -> Scp327ClientHooks.lure(Payload));
        Registrar.playToClient(Scp327HeldPayload.PayloadType, Scp327HeldPayload.PayloadCodec, (Payload, Context) -> Scp327ClientHooks.held(Payload));
        Registrar.playToServer(Scp327StrugglePayload.PayloadType, Scp327StrugglePayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                Scp327Hold.struggle(User);
            }
        });
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent Event) {
        LivingEntity Victim = Event.getEntity();
        if (!(Victim.level() instanceof ServerLevel World) || !Event.getSource().is(Scp327Registry.Drowned)) {
            return;
        }
        if (Event.getSource().getEntity() instanceof Scp327Siren Siren) {
            if (Victim instanceof Player || ControlBodies.isPersonLike(Victim)) {
                Siren.recordKill(World);
            }
            if (Siren.mode() == Scp327Siren.ModeDrag) {
                Siren.endDrag(World, false);
            }
        }
    }

    @SubscribeEvent
    public static void walled(LivingIncomingDamageEvent Event) {
        LivingEntity Victim = Event.getEntity();
        if (!Event.getSource().is(DamageTypes.IN_WALL) || !(Victim.level() instanceof ServerLevel World) || !Scp327Hold.isHeld(Victim)) {
            return;
        }
        Event.setCanceled(true);
        Scp327Hold.unstick(World, Victim, Victim.position().add(0.0, 1.0, 0.0));
    }

    @SubscribeEvent
    public static void splashed(ProjectileImpactEvent Event) {
        if (!(Event.getProjectile() instanceof ThrownPotion Potion) || !(Potion.level() instanceof ServerLevel World)) {
            return;
        }
        PotionContents Contents = Potion.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (!Contents.is(Potions.WATER)) {
            return;
        }
        Vec3 Where = Event.getRayTraceResult().getLocation();
        for (Scp327Siren Creature : World.getEntitiesOfClass(Scp327Siren.class, new AABB(Where, Where).inflate(SplashReach), LivingEntity::isAlive)) {
            Creature.rehydrate();
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp327Lure.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Player User = Event.getEntity();
        if (Scp327Hold.isHeld(User)) {
            Scp327Hold.release(User);
        }
        Scp327Lure.forget(User);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp327Lure.clear();
        Scp327Hold.clear();
    }
}
