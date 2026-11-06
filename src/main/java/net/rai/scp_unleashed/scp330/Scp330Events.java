package net.rai.scp_unleashed.scp330;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp330.client.Scp330ClientHooks;
import net.rai.scp_unleashed.scp330.network.Scp330MarkPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp330Events {
    private Scp330Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp330MarkPayload.PayloadType, Scp330MarkPayload.PayloadCodec, (Payload, Context) -> Scp330ClientHooks.mark(Payload));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp330Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            for (Scp330Colour Colour : Scp330Colour.values()) {
                Event.accept(Scp330Registry.candy(Colour));
            }
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp330Registry.BowlItem.get());
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Scp330Marks.levelTick(World);
        }
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        Scp330Marks.joined(Event.getEntity());
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking Event) {
        if (Event.getEntity() instanceof ServerPlayer Viewer) {
            Scp330Marks.startTracking(Viewer, Event.getTarget());
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp330Marks.resend(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp330Marks.resend(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp330Marks.resend(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void heavyHand(LivingIncomingDamageEvent Event) {
        LivingEntity Target = Event.getEntity();
        DamageSource Source = Event.getSource();
        if (Source.getEntity() instanceof LivingEntity Attacker && Attacker != Target && Attacker.hasEffect(Scp330Registry.Wrath)) {
            Event.setAmount(Event.getAmount() * Scp330Candies.WrathMultiplier);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shell(LivingIncomingDamageEvent Event) {
        LivingEntity Target = Event.getEntity();
        if (Event.getSource().is(Scp330Registry.GreedDamage)) {
            Event.setInvulnerabilityTicks(0);
            return;
        }
        float Amount = Event.getAmount();
        float Left = Scp330Barrier.absorb(Target, Event.getSource(), Amount);
        if (Left >= Amount) {
            return;
        }
        if (Left <= 0.0f) {
            Event.setCanceled(true);
        } else {
            Event.setAmount(Left);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void heal(LivingHealEvent Event) {
        if (Scp330Greed.isGreedy(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Target = Event.getEntity();
        if (Target.level().isClientSide) {
            return;
        }
        if (Scp330Greed.isGreedy(Target)) {
            Scp330Greed.cure(Target);
        }
        if (Target.hasData(Scp330Registry.Shell)) {
            Scp330Barrier.clear(Target);
        }
    }

    @SubscribeEvent
    public static void effectRemoved(MobEffectEvent.Remove Event) {
        if (Event.getEffect().is(Scp330Registry.Barrier.getKey()) && !Event.getEntity().level().isClientSide) {
            Scp330Barrier.clear(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void effectExpired(MobEffectEvent.Expired Event) {
        if (Event.getEffectInstance() != null && Event.getEffectInstance().getEffect().is(Scp330Registry.Barrier.getKey()) && !Event.getEntity().level().isClientSide) {
            Scp330Barrier.clear(Event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void pickup(ItemEntityPickupEvent.Pre Event) {
        Player User = Event.getPlayer();
        if (!User.level().isClientSide && Scp330Greed.isGreedy(User)) {
            Event.setCanPickup(TriState.FALSE);
            Scp330Greed.pickUp(User, Event.getItemEntity());
        }
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock Event) {
        Player User = Event.getEntity();
        if (!User.hasEffect(Scp330Registry.Bloom) || !Scp330Bloom.growable(Event.getLevel(), Event.getPos())) {
            return;
        }
        if (Event.getHand() == InteractionHand.MAIN_HAND) {
            Scp330Bloom.use(User, Event.getLevel(), Event.getPos());
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.sidedSuccess(Event.getLevel().isClientSide));
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp330Bloom.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp330Bowls.clear();
        Scp330Marks.clear();
        Scp330Bloom.clear();
    }
}
