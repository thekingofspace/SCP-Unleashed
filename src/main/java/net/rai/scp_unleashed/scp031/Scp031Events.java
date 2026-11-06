package net.rai.scp_unleashed.scp031;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp031Events {
    private Scp031Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp031Registry.Mimic.get(), Scp031Mimic.createAttributes().build());
        Event.put(Scp031Registry.Polyp.get(), Scp031Polyp.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp031Registry.MimicEgg.get());
            Event.accept(Scp031Registry.PolypEgg.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Victim = Event.getEntity();
        if (Event.isCanceled() || !(Victim.level() instanceof ServerLevel World)) {
            return;
        }
        if (Scp031Holds.isHeld(Victim)) {
            Scp031Holds.release(Victim);
        }
        if (Event.getSource().is(Scp031Registry.EmbraceDamage) && Scp031Targets.isPerson(Victim)) {
            Scp031Polyp.sprout(World, Victim.position());
        }
    }

    @SubscribeEvent
    public static void heal(LivingHealEvent Event) {
        if (!Event.getEntity().level().isClientSide && Scp031Holds.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent Event) {
        if (Event.getEntity().level().isClientSide || !(Event.getTarget() instanceof Scp031Mimic Holder) || !Scp031Holds.isHeldBy(Event.getEntity(), Holder)) {
            return;
        }
        Event.setCanceled(true);
        Event.getEntity().displayClientMessage(Component.translatable("message.scp_unleashed.scp031.pinned").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Scp031Holds.isHeld(Event.getEntity())) {
            Scp031Holds.release(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp031Holds.sweep(Event.getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp031Holds.clear();
    }
}
