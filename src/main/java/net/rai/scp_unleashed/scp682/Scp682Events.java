package net.rai.scp_unleashed.scp682;

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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.stasis.StasisEffects;
import net.rai.scp_unleashed.stasis.StasisPodItem;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp682Events {
    public static final String StasisRefusal = "message.scp_unleashed.scp682.stasis";

    private Scp682Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp682Registry.Scp682Type.get(), Scp682.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp682Registry.Remover.get());
            Event.accept(Scp682Registry.Egg.get());
        }
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (!Event.getLevel().isClientSide) {
            Scp682Rivals.joined(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void effectApplicable(MobEffectEvent.Applicable Event) {
        if (Event.getEntity() instanceof Scp682 Reptile && !Reptile.level().isClientSide && Reptile.refuses(Event.getEffectInstance().getEffect())) {
            Event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void stasisAttempt(PlayerInteractEvent.EntityInteract Event) {
        ItemStack Stack = Event.getItemStack();
        if (!(Stack.getItem() instanceof StasisPodItem Pod) || !(Event.getTarget() instanceof Scp682 Reptile) || !Scp682.refusesStasis(Reptile)
                || StasisPodItem.isOccupied(Stack)) {
            return;
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.CONSUME);
        if (Event.getLevel() instanceof ServerLevel World) {
            Event.getEntity().displayClientMessage(Component.translatable(StasisRefusal).withStyle(ChatFormatting.RED), true);
            StasisEffects.deny(World, Reptile.getBoundingBox().getCenter(), Pod.getGrade());
        }
    }
}
