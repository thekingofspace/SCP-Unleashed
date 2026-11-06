package net.rai.scp_unleashed.archive.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.SealedDocumentItem;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentStamp;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public class ArchiveClientEvents {
    public static final int UntintedColour = 0xFFFFFFFF;

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        SealedDocumentItem.setRevealCheck(() -> Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(ArchiveRegistry.ArchiveMenuType.get(), ArchiveScreen::new);
    }

    @SubscribeEvent
    public static void registerItemColours(RegisterColorHandlersEvent.Item Event) {
        Event.register(ArchiveClientEvents::documentTint, ArchiveRegistry.SealedDocument.get(), ArchiveRegistry.OpenedDocument.get(), ArchiveRegistry.DatabaseCopy.get());
    }

    @SubscribeEvent
    public static void forgetDocuments(ClientPlayerNetworkEvent.LoggingOut Event) {
        AnomalyDocuments.clearClient();
        DatabaseRecords.clear();
    }

    private static int documentTint(ItemStack Stack, int TintIndex) {
        if (TintIndex != 1) {
            return UntintedColour;
        }
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        return Stamp == null ? 0xFF9A9A90 : Stamp.Classification().getColour() | 0xFF000000;
    }
}
