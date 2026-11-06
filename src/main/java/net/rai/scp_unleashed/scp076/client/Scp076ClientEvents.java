package net.rai.scp_unleashed.scp076.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp076.Scp076Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp076ClientEvents {
    private Scp076ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp076Registry.Abel.get(), Scp076AbelRenderer::new);
        Event.registerEntityRenderer(Scp076Registry.Knife.get(), Scp076KnifeRenderer::new);
        Event.registerEntityRenderer(Scp076Registry.Blades.get(), Scp076BladesRenderer::new);
        Event.registerBlockEntityRenderer(Scp076Registry.DoorEntity.get(), Scp076DoorRenderer::new);
        Event.registerBlockEntityRenderer(Scp076Registry.CoffinEntity.get(), Scp076CoffinRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp076Models.all()) {
            Event.register(Model);
        }
    }
}
