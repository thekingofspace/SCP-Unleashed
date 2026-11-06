package net.rai.scp_unleashed.speaker;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.registry.ModItems;
import net.rai.scp_unleashed.speaker.cassette.AnomalousLibrary;
import net.rai.scp_unleashed.speaker.cassette.CassetteItem;
import net.rai.scp_unleashed.speaker.cassette.CassetteLibraryPayload;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public class SpeakerEvents {
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(SoundConsolePayload.PayloadType, SoundConsolePayload.PayloadCodec, SoundConsolePayload::handle);
        Registrar.playToClient(CassetteLibraryPayload.PayloadType, CassetteLibraryPayload.PayloadCodec,
                (Payload, Context) -> AnomalousLibrary.acceptClient(Payload.Library()));
    }

    @SubscribeEvent
    public static void syncLibrary(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Listener) {
            AnomalousLibrary.sync(Listener);
        }
    }

    @SubscribeEvent
    public static void fillCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Machines.getKey()) {
            return;
        }
        List<ItemStack> Listed = new ArrayList<>();
        Listed.add(new ItemStack(SpeakerRegistry.SoundConsoleItem.get()));
        for (SoundCatalog.Entry Entry : SoundCatalog.inCategory(SoundCatalog.Category.Anomalous)) {
            Listed.add(CassetteItem.create(Entry));
        }
        Listed.add(new ItemStack(SpeakerRegistry.SpeakerItem.get()));
        ItemStack Anchor = new ItemStack(ModItems.PortRange.get());
        CreativeModeTab.TabVisibility Visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        boolean Anchored = Event.getParentEntries().contains(Anchor) && Event.getSearchEntries().contains(Anchor);
        for (ItemStack Stack : Listed) {
            if (Anchored) {
                Event.insertAfter(Anchor, Stack, Visibility);
                Anchor = Stack;
            } else {
                Event.accept(Stack, Visibility);
            }
        }
    }
}
