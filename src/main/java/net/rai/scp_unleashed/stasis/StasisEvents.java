package net.rai.scp_unleashed.stasis;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.stasis.client.StasisClientHooks;
import net.rai.scp_unleashed.stasis.network.StasisEffectPayload;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class StasisEvents {
    private static final Set<UUID> FreshMenus = new HashSet<>();

    private StasisEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(StasisEffectPayload.PayloadType, StasisEffectPayload.PayloadCodec, (Payload, Context) -> StasisClientHooks.effect(Payload));
    }

    @SubscribeEvent
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Anomalies.getKey()) {
            for (FragmentRarity Grade : FragmentRarity.values()) {
                Event.accept(StasisRegistry.podFor(Grade));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interactEntity(PlayerInteractEvent.EntityInteract Event) {
        ItemStack Stack = Event.getItemStack();
        if (!StasisPodItem.isPod(Stack) || !(Event.getTarget() instanceof LivingEntity Target)) {
            return;
        }
        Player User = Event.getEntity();
        Component Refusal = StasisPods.refusal(User, Stack, Target);
        Event.setCanceled(true);
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            Event.setCancellationResult(Refusal == null ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
            return;
        }
        if (Refusal != null) {
            User.displayClientMessage(Refusal, true);
            StasisEffects.deny(World, Target.getBoundingBox().getCenter(), ((StasisPodItem) Stack.getItem()).getGrade());
            Event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }
        Event.setCancellationResult(Target instanceof Mob Creature && StasisPods.capture(World, User, Stack, Creature) ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
    }

    @SubscribeEvent
    public static void containerOpened(PlayerContainerEvent.Open Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            FreshMenus.add(User.getUUID());
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        boolean Due = User.tickCount % StasisScans.ScanInterval == 0;
        boolean Fresh = !FreshMenus.isEmpty() && FreshMenus.remove(User.getUUID());
        if (Fresh || Due) {
            StasisScans.scanMenu(User);
        }
        if (Due) {
            StasisScans.scanPlayer(User);
        }
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        Entity Ticked = Event.getEntity();
        if (Ticked.level().isClientSide || Ticked instanceof Player || !(Ticked instanceof LivingEntity || Ticked instanceof ItemFrame)) {
            return;
        }
        if ((Ticked.tickCount + Ticked.getId()) % StasisScans.ScanInterval != 0) {
            return;
        }
        StasisScans.scanCarrier(Ticked);
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        FreshMenus.clear();
    }
}
