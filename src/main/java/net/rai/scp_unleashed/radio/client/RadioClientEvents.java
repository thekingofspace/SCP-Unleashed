package net.rai.scp_unleashed.radio.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.radio.RadioCarry;
import net.rai.scp_unleashed.radio.RadioRegistry;
import net.rai.scp_unleashed.radio.network.RadioTunePayload;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class RadioClientEvents {
    public static final ResourceLocation OnProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "radio_on");
    public static final ResourceLocation KeyedProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "radio_keyed");

    private RadioClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(RadioRegistry.Radio.get(), OnProperty, (Stack, World, Holder, Seed) -> RadioCarry.isOn(Stack) ? 1.0f : 0.0f);
            ItemProperties.register(RadioRegistry.Radio.get(), KeyedProperty, (Stack, World, Holder, Seed) -> keyedInThirdPerson(Stack, Holder) ? 1.0f : 0.0f);
        });
    }

    public static boolean keyedInThirdPerson(ItemStack Stack, @Nullable LivingEntity Holder) {
        if (Holder == null || !Holder.isUsingItem() || Holder.getUseItem() != Stack) {
            return false;
        }
        Minecraft Client = Minecraft.getInstance();
        return Holder != Client.player || !Client.options.getCameraType().isFirstPerson();
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, RadioGlitchOverlay.LayerId, RadioGlitchOverlay::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        RadioGlitchOverlay.tick();
    }

    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent Event) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User == null || Client.screen != null || !User.isShiftKeyDown() || User.isSpectator()) {
            return;
        }
        if (!RadioCarry.isRadio(User.getItemInHand(InteractionHand.MAIN_HAND))) {
            return;
        }
        double Delta = Event.getScrollDeltaY();
        if (Delta == 0.0) {
            return;
        }
        Event.setCanceled(true);
        PacketDistributor.sendToServer(new RadioTunePayload(Delta > 0.0 ? 1 : -1));
    }
}
