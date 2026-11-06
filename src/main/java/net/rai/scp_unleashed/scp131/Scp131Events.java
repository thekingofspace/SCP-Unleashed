package net.rai.scp_unleashed.scp131;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp131.network.Scp131GriefPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp131Events {
    public static final float TollVolume = 0.55f;
    public static final float TollPitch = 0.8f;

    private Scp131Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp131Registry.Pod.get(), Scp131Pod.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp131GriefPayload.PayloadType, Scp131GriefPayload.PayloadCodec,
                (Payload, Context) -> Context.enqueueWork(() -> Scp131Grief.setClientUntil(Payload.Until())));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp131Registry.PodEgg.get());
            for (Scp131Colour Colour : Scp131Colour.values()) {
                Event.accept(new Scp131PodData(Colour, (float) Scp131Pod.Health, 0).stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent Event) {
        if (Event.isCanceled() || !(Event.getEntity() instanceof Scp131Pod Pod) || !(Pod.level() instanceof ServerLevel World)) {
            return;
        }
        Entity Killer = Event.getSource().getEntity();
        Entity Blamed = Killer instanceof LivingEntity && !(Killer instanceof Scp131Pod) ? Killer : null;
        Scp131Grief.get(World.getServer()).mourn(World, Pod.position(), Blamed);
        World.playSound(null, Pod.getX(), Pod.getY() + 0.3, Pod.getZ(), Scp131Registry.DeathToll.get(), SoundSource.NEUTRAL, TollVolume, TollPitch * Pod.voicePitch());
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp131Grief.get(User.server).sendTo(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp131Grief.get(User.server).sendTo(User);
        }
    }
}
