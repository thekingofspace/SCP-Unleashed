package net.rai.scp_unleashed.stasis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.stasis.StasisClock;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class StasisClientEvents {
    public static final ResourceLocation OccupiedProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "stasis_occupied");

    private StasisClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            for (FragmentRarity Grade : FragmentRarity.values()) {
                ItemProperties.register(StasisRegistry.podFor(Grade), OccupiedProperty,
                        (Stack, World, Holder, Seed) -> StasisPodItem.isOccupied(Stack) ? 1.0F : 0.0F);
            }
        });
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level != null) {
            StasisClock.setClient(Game.level.getGameTime());
        }
    }
}
