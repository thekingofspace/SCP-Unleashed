package net.rai.scp_unleashed.scp096;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp096.network.Scp096SeenPayload;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp096Events {
    private Scp096Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp096Registry.ShyGuy.get(), Scp096ShyGuy.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp096Registry.SpawnEgg.get());
            Event.accept(Scp096Registry.Bag.get());
        }
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(Scp096Registry.Goggles.get());
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(Scp096SeenPayload.PayloadType, Scp096SeenPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                seen(User, Payload);
            }
        });
    }

    private static void seen(ServerPlayer User, Scp096SeenPayload Payload) {
        ServerLevel World = User.serverLevel();
        if (World.getEntity(Payload.EntityId()) instanceof Scp096ShyGuy Guy) {
            Guy.reportSighting(World, User);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Body = Event.getEntity();
        if (Body.level().isClientSide || Body instanceof Scp096ShyGuy) {
            return;
        }
        UUID Key = Body.getUUID();
        for (Scp096ShyGuy Guy : List.copyOf(Scp096ShyGuy.active())) {
            if (Guy.level() == Body.level() || Guy.level().getServer() == Body.level().getServer()) {
                Guy.forget(Key);
            }
        }
    }
}
