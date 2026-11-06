package net.rai.scp_unleashed.sealedcase.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.sealedcase.SealedCaseItem;
import net.rai.scp_unleashed.sealedcase.SealedCaseRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class SealedCaseClientEvents {
    public static final ResourceLocation SealedProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "sealed");

    private SealedCaseClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> ItemProperties.register(SealedCaseRegistry.SealedCase.get(), SealedProperty,
                (Stack, World, Owner, Seed) -> SealedCaseItem.isSealed(Stack) ? 1.0f : 0.0f));
    }
}
