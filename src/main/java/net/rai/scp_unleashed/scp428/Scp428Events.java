package net.rai.scp_unleashed.scp428;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp428.client.Scp428ClientHooks;
import net.rai.scp_unleashed.scp428.network.Scp428HoldPayload;
import net.rai.scp_unleashed.scp428.network.Scp428StrugglePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp428Events {
    private Scp428Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp428Registry.Core.get(), Scp428Core.createAttributes().build());
        Event.put(Scp428Registry.Instance.get(), Scp428Instance.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(Scp428StrugglePayload.PayloadType, Scp428StrugglePayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> {
            if (Context.player() instanceof ServerPlayer User) {
                Scp428Holds.press(User);
            }
        }));
        Registrar.playToClient(Scp428HoldPayload.PayloadType, Scp428HoldPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> Scp428ClientHooks.hold(Payload)));
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp428Holds.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp428Registry.CoreEgg.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Victim = Event.getEntity();
        if (Victim.level().isClientSide || !Event.getSource().is(Scp428Registry.AbsorbDamage) || !(Event.getSource().getEntity() instanceof Scp428Core Core)) {
            return;
        }
        if (Victim instanceof Scp428Core || Victim instanceof Scp428Instance) {
            return;
        }
        Core.absorbed(Victim);
    }
}
