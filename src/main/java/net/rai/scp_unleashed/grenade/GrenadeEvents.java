package net.rai.scp_unleashed.grenade;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.grenade.client.GrenadeClientHooks;
import net.rai.scp_unleashed.grenade.network.GrenadeFlashPayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class GrenadeEvents {
    private GrenadeEvents() {
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent Event) {
        Event.enqueueWork(() -> {
            for (DeferredItem<GrenadeItem> Entry : GrenadeRegistry.creativeOrder()) {
                DispenserBlock.registerProjectileBehavior(Entry.get());
            }
        });
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(GrenadeFlashPayload.PayloadType, GrenadeFlashPayload.PayloadCodec, (Payload, Context) -> GrenadeClientHooks.onFlash(Payload));
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Weapons.getKey()) {
            return;
        }
        for (DeferredItem<GrenadeItem> Entry : GrenadeRegistry.creativeOrder()) {
            Event.accept(new ItemStack(Entry.get()));
        }
    }

    @SubscribeEvent
    public static void retarget(LivingChangeTargetEvent Event) {
        if (Event.getNewAboutToBeSetTarget() != null && GrenadeDaze.isDazed(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        GrenadeDaze.clear();
    }
}
