package net.rai.scp_unleashed.scp500.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp500.Scp500BottleItem;
import net.rai.scp_unleashed.scp500.Scp500Registry;
import net.rai.scp_unleashed.scp500.Scp500Stock;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp500ClientEvents {
    public static final ResourceLocation MissingProperty = Scp500Registry.location("scp500_missing");
    public static final ResourceLocation FallbackProperty = Scp500Registry.location("scp500_fallback");

    private Scp500ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(Scp500Registry.BottleItem.get(), MissingProperty, (Stack, World, Holder, Seed) -> Scp500Stock.Full - Scp500BottleItem.pills(Stack));
            ItemProperties.register(Scp500Registry.BottleItem.get(), FallbackProperty, (Stack, World, Holder, Seed) -> Holder == null && ShaderCompat.useFallback() ? 1.0f : 0.0f);
            Scp500Display.register();
        });
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(Scp500HeldExtensions.Instance, Scp500Registry.Pill.get(), Scp500Registry.BottleItem.get());
    }
}
