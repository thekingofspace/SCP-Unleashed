package net.rai.scp_unleashed.scp939;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp939.client.Scp939ClientHooks;
import net.rai.scp_unleashed.scp939.network.Scp939BreathPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp939Events {
    private Scp939Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp939Registry.Scp939Type.get(), Scp939.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Scp939Registry.CompressorEntity.get(), (Machine, Side) -> Machine.getItemHandler(Side));
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp939BreathPayload.PayloadType, Scp939BreathPayload.PayloadCodec, (Payload, Context) -> Scp939ClientHooks.breath(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp939Registry.Egg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(Scp939Registry.CompressorItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(Scp939Registry.EmptyCanister.get());
            Event.accept(Scp939Registry.Canister.get());
        }
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        if (Event.getEntity() instanceof Villager Child && Child.level() instanceof ServerLevel World && Child.hasData(Scp939Registry.Brood)) {
            Scp939Brood.tick(World, Child);
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp939Breath.tick(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void incomingDamage(LivingIncomingDamageEvent Event) {
        if (Scp939Brood.shieldsFromCold(Event.getEntity(), Event.getSource())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent Event) {
        if (Event.getEntity() instanceof Animal && Event.getSource().getEntity() instanceof Scp939) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void experience(LivingExperienceDropEvent Event) {
        if (Event.getEntity() instanceof Animal && Event.getAttackingPlayer() == null && Event.getEntity().getLastHurtByMob() instanceof Scp939) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof Player User && !User.level().isClientSide) {
            Scp939Breath.reset(User);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        resend(Event.getEntity());
    }

    private static void resend(Player Target) {
        if (Target instanceof ServerPlayer User) {
            Scp939Breath.resend(User);
        }
    }

    @SubscribeEvent
    public static void blockBroken(BlockEvent.BreakEvent Event) {
        forget(Event.getLevel(), Event.getPos());
    }

    @SubscribeEvent
    public static void blockPlaced(BlockEvent.EntityPlaceEvent Event) {
        forget(Event.getLevel(), Event.getPos());
    }

    private static void forget(LevelAccessor Accessor, BlockPos Pos) {
        if (Accessor instanceof ServerLevel World) {
            Scp939Rooms.forgetNear(World, Pos);
        }
    }

    @SubscribeEvent
    public static void levelUnloaded(LevelEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Scp939Rooms.forget(World);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp939Rooms.forgetAll();
    }
}
