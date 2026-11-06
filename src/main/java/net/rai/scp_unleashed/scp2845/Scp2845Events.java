package net.rai.scp_unleashed.scp2845;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
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
import net.rai.scp_unleashed.scp2845.client.Scp2845ClientHooks;
import net.rai.scp_unleashed.scp2845.network.Scp2845ImpactPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp2845Events {
    private Scp2845Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp2845Registry.DeerType.get(), Scp2845.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp2845ImpactPayload.PayloadType, Scp2845ImpactPayload.PayloadCodec, (Payload, Context) -> Scp2845ClientHooks.impact(Payload));
    }

    @SubscribeEvent
    public static void chickenSlain(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof Chicken Bird && Bird.level() instanceof ServerLevel World && Event.getSource().getEntity() instanceof Player Giver) {
            Scp2845.offering(World, Bird.position(), Giver);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp2845Registry.Egg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp2845Registry.StoneItem.get());
            Event.accept(Scp2845Registry.PillarItem.get());
        }
    }
}
