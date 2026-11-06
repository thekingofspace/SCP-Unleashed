package net.rai.scp_unleashed.scp001;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp001.client.Scp001ClientHooks;
import net.rai.scp_unleashed.scp001.network.Scp001GlarePayload;
import net.rai.scp_unleashed.stasis.StasisEffects;
import net.rai.scp_unleashed.stasis.StasisPodItem;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp001Events {
    public static final String SlipKey = "message.scp_unleashed.scp001.slips";

    private Scp001Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp001Registry.Prototype.get(), Scp001Prototype.createAttributes().build());
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp001Registry.SpawnEgg.get());
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp001GlarePayload.PayloadType, Scp001GlarePayload.PayloadCodec, (Payload, Context) -> Scp001ClientHooks.glare(Payload));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void podTouch(PlayerInteractEvent.EntityInteract Event) {
        ItemStack Stack = Event.getItemStack();
        if (!StasisPodItem.isPod(Stack) || !(Event.getTarget() instanceof Scp001Prototype Creature) || Creature.isLocked()) {
            return;
        }
        if (StasisPodItem.isOccupied(Stack) || StasisPodItem.remaining(Stack, Event.getLevel().getGameTime()) <= 0L) {
            return;
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.CONSUME);
        if (Event.getLevel() instanceof ServerLevel World) {
            Event.getEntity().displayClientMessage(Component.translatable(SlipKey).withStyle(ChatFormatting.RED), true);
            StasisEffects.deny(World, Creature.getBoundingBox().getCenter(), ((StasisPodItem) Stack.getItem()).getGrade());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void refuseDeath(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof Scp001Prototype Creature && !Scp001Prototype.finishes(Event.getSource())) {
            Event.setCanceled(true);
            Creature.holdFloor();
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Scp001Returns.get(World).tick(World);
        }
    }
}
