package net.rai.scp_unleashed.computer.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.computer.ComputerRegistry;
import net.rai.scp_unleashed.computer.FloppyDiscItem;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class ComputerClientEvents {
    public static final ResourceLocation WrittenProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "written");

    private ComputerClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> ItemProperties.register(ComputerRegistry.FloppyDisc.get(), WrittenProperty,
                (Stack, World, Holder, Seed) -> FloppyDiscItem.contentsOf(Stack).isEmpty() ? 0.0f : 1.0f));
    }
}
