package net.rai.scp_unleashed.microhid.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.client.GunsClientEvents;
import net.rai.scp_unleashed.microhid.MicroChargingPlatformBlockEntity;
import net.rai.scp_unleashed.microhid.MicroHidEnergy;
import net.rai.scp_unleashed.microhid.MicroHidRegistry;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class MicroHidClientEvents {
    public static final ResourceLocation StateProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "micro_hid_state");
    public static final ResourceLocation PanelLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "micro_hid_panel");
    public static final float Empty = 0.0F;
    public static final float Idle = 0.2F;
    public static final float SpinLow = 0.4F;
    public static final float SpinHigh = 0.6F;
    public static final float Fire = 0.8F;
    public static final int ChargingSeed = 0x4D484944;
    public static final int PulseTicks = 8;

    private MicroHidClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> ItemProperties.register(MicroHidRegistry.MicroHid.get(), StateProperty, MicroHidClientEvents::state));
        MicroChargingPlatformBlockEntity.setClientEffects(MicroChargingPlatformEffects::tick);
    }

    public static float state(ItemStack Stack, @Nullable ClientLevel World, @Nullable LivingEntity Holder, int Seed) {
        if (Seed == ChargingSeed) {
            ClientLevel Current = World != null ? World : Minecraft.getInstance().level;
            long Time = Current == null ? 0L : Current.getGameTime();
            return (Time / PulseTicks) % 2 == 0 ? SpinLow : SpinHigh;
        }
        if (Holder != null && ItemStack.isSameItem(Holder.getMainHandItem(), Stack)) {
            MicroHidEffects.Phase Current = MicroHidEffects.phaseOf(Holder);
            if (Current == MicroHidEffects.Phase.Fire) {
                return Fire;
            }
            if (Current == MicroHidEffects.Phase.Spin) {
                return MicroHidEffects.spinProgress(Holder, 0.0F) < 0.5F ? SpinLow : SpinHigh;
            }
            if (Current == MicroHidEffects.Phase.Wind) {
                return MicroHidEffects.windProgress(Holder, 0.0F) < 0.5F ? SpinHigh : SpinLow;
            }
        }
        return MicroHidEnergy.canFire(Stack) ? Idle : Empty;
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(MicroHidRegistry.PlatformEntity.get(), MicroChargingPlatformRenderer::new);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(GunsClientEvents.AmmoLayer, PanelLayer, MicroHidHud::renderPanel);
    }
}
