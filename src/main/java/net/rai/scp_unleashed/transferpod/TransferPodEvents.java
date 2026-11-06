package net.rai.scp_unleashed.transferpod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class TransferPodEvents {
    public static final String Neighbour = "stasis_pod_abyssal";

    private TransferPodEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Anomalies.getKey()) {
            return;
        }
        ItemStack Anchor = neighbour(Event);
        for (TransferPodContent Held : TransferPods.all()) {
            if (!Held.Listed()) {
                continue;
            }
            ItemStack Entry = TransferPods.loaded(Held.Id());
            if (Anchor != null) {
                Event.insertAfter(Anchor, Entry, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                Anchor = Entry;
            } else {
                Event.accept(Entry);
            }
        }
    }

    private static ItemStack neighbour(BuildCreativeModeTabContentsEvent Event) {
        ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Neighbour);
        if (!BuiltInRegistries.ITEM.containsKey(Id)) {
            return null;
        }
        ItemStack Wanted = new ItemStack(BuiltInRegistries.ITEM.get(Id));
        return Event.getParentEntries().contains(Wanted) && Event.getSearchEntries().contains(Wanted) ? Wanted : null;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interactEntity(PlayerInteractEvent.EntityInteract Event) {
        ItemStack Stack = Event.getItemStack();
        if (!TransferPods.isPod(Stack) || !(Event.getTarget() instanceof LivingEntity Target)) {
            return;
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.CONSUME);
        if (Event.getLevel() instanceof ServerLevel World) {
            Event.getEntity().displayClientMessage(Component.translatable(TransferPods.MessagePrefix + "refill").withStyle(ChatFormatting.GRAY), true);
            TransferPodEffects.deny(World, Target.getBoundingBox().getCenter());
        }
    }
}
