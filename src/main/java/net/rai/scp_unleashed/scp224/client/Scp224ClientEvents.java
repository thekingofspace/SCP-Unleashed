package net.rai.scp_unleashed.scp224.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp224.Scp224Aging;
import net.rai.scp_unleashed.scp224.Scp224Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp224ClientEvents {
    private Scp224ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp224Registry.ClockEntity.get(), Scp224ClockRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ResourceLocation Location : Scp224ClockRenderer.Models) {
            Event.register(ModelResourceLocation.standalone(Location));
        }
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerBelowAll(Scp224Vision.LayerId, Scp224Vision::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp224Vision.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void movementInput(MovementInputUpdateEvent Event) {
        if (Event.getEntity() instanceof LocalPlayer User && Scp224ClientState.isForced() && Scp224Aging.canStoop(User)) {
            Event.getInput().shiftKeyDown = true;
        }
    }

    @SubscribeEvent
    public static void fov(ComputeFovModifierEvent Event) {
        AttributeInstance Speed = Event.getPlayer().getAttribute(Attributes.MOVEMENT_SPEED);
        float Walking = Event.getPlayer().getAbilities().getWalkingSpeed();
        if (Speed == null || Walking <= 0.0f) {
            return;
        }
        AttributeModifier Ours = Speed.getModifier(Scp224Registry.SlowId);
        if (Ours == null || Ours.amount() <= -1.0) {
            return;
        }
        double With = Speed.getValue() / Walking;
        double Without = With / (1.0 + Ours.amount());
        Event.setNewFovModifier((float) (Event.getNewFovModifier() * (Without + 1.0) / (With + 1.0)));
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp224Vision.reset();
    }
}
