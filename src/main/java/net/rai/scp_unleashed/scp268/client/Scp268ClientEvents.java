package net.rai.scp_unleashed.scp268.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.scp268.Scp268Registry;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp268ClientEvents {
    public static final String Ignored = "message.scp_unleashed.scp268_cap.ignored";
    public static final int NoticeTicks = 60;
    private static int Quiet;

    private Scp268ClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tickStart(ClientTickEvent.Pre Event) {
        if (Quiet > 0) {
            Quiet--;
        }
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (User == null || !Scp268.isNeglected(User)) {
            return;
        }
        boolean Tried = false;
        for (KeyMapping Button : List.of(Game.options.keyAttack, Game.options.keyUse)) {
            while (Button.consumeClick()) {
                Tried = true;
            }
            if (Button.isDown()) {
                Button.setDown(false);
                Tried = true;
            }
        }
        if (Tried && Quiet == 0) {
            Quiet = NoticeTicks;
            User.displayClientMessage(Component.translatable(Ignored).withStyle(ChatFormatting.GRAY), true);
        }
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp268Display::register);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp268Display.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        Event.registerLayerDefinition(Scp268CapLayer.Location, Scp268CapLayer::create);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers Event) {
        Scp268CapLayer.bake(Event.getEntityModels());
        for (PlayerSkin.Model Skin : Event.getSkins()) {
            if (Event.getSkin(Skin) instanceof PlayerRenderer Renderer) {
                Renderer.addLayer(new Scp268WornLayer(Renderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot, HumanoidModel<?> Original) {
                return Scp268CapLayer.model();
            }
        }, Scp268Registry.Cap.get());
    }
}
