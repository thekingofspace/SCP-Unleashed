package net.rai.scp_unleashed.scp127;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp127.network.Scp127DryPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp127Events {
    private Scp127Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(Scp127DryPayload.PayloadType, Scp127DryPayload.PayloadCodec, Scp127DryPayload::handle);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp127Item.create());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Victim = Event.getEntity();
        if (Victim.level().isClientSide || Victim instanceof ArmorStand) {
            return;
        }
        DamageSource Source = Event.getSource();
        if (!Source.is(GunsRegistry.GunshotDamage) || !(Source.getEntity() instanceof ServerPlayer Shooter) || Shooter == Victim) {
            return;
        }
        ItemStack Held = Shooter.getMainHandItem();
        if (Scp127Item.is(Held)) {
            Scp127Life.fed(Shooter, Held);
        }
    }

    @SubscribeEvent
    public static void equipped(LivingEquipmentChangeEvent Event) {
        if (Event.getSlot() != EquipmentSlot.MAINHAND || !(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        boolean Before = Scp127Item.is(Event.getFrom());
        boolean After = Scp127Item.is(Event.getTo());
        if (!Before && After) {
            Scp127Voice.say(User, Scp127Voice.Topic.Equip);
        } else if (Before && !After) {
            Scp127Voice.say(User, Scp127Voice.Topic.PutAway);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp127Voice.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp127Voice.forgetAll();
    }
}
