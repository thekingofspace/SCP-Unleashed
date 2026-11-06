package net.rai.scp_unleashed.scp127.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.client.GunClientState;
import net.rai.scp_unleashed.guns.client.GunsClientEvents;
import net.rai.scp_unleashed.scp127.Scp127Item;
import net.rai.scp_unleashed.scp127.network.Scp127DryPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp127ClientEvents {
    public static final long DryResend = 20L;
    private static long LastDry = Long.MIN_VALUE / 2;

    private Scp127ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp127Display::register);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp127Display.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(GunsClientEvents.AmmoLayer, Scp127Hud.Layer, Scp127Hud::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (User == null || Game.level == null || Game.screen != null || User.isSpectator() || !Game.options.keyAttack.isDown()) {
            return;
        }
        ItemStack Stack = User.getMainHandItem();
        if (!Scp127Item.is(Stack) || GunClientState.predictedAmmo(User, Stack) > 0) {
            return;
        }
        long Now = Game.level.getGameTime();
        if (Now - LastDry < DryResend && Now >= LastDry) {
            return;
        }
        LastDry = Now;
        PacketDistributor.sendToServer(Scp127DryPayload.Instance);
    }
}
