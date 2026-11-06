package net.rai.scp_unleashed.ultralaser.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunTimer;
import net.rai.scp_unleashed.guns.client.GunsClientEvents;
import net.rai.scp_unleashed.ultralaser.UltralaserRegistry;
import net.rai.scp_unleashed.ultralaser.UltralaserScore;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class UltralaserClientEvents {
    public static final ResourceLocation StateProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "ultralaser_state");
    public static final ResourceLocation PanelLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "ultralaser_panel");
    public static final ResourceLocation ReticleLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "ultralaser_reticle");
    public static final float Empty = 0.0F;
    public static final float Idle = 0.15F;
    public static final float Hot = 0.65F;
    public static final float HotShare = 0.5F;
    public static final int[] StageTicks = {0, UltralaserScore.BuildTick, UltralaserScore.CountTick, UltralaserScore.ThreeTick};

    private UltralaserClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> ItemProperties.register(UltralaserRegistry.Ultralaser.get(), StateProperty, UltralaserClientEvents::state));
    }

    public static float state(ItemStack Stack, @Nullable ClientLevel World, @Nullable LivingEntity Holder, int Seed) {
        GunTimer Charging = GunItem.getCharging(Stack);
        if (Charging != null) {
            ClientLevel Current = World != null ? World : Minecraft.getInstance().level;
            float Ticks = Holder instanceof Player User ? UltralaserPrimingSound.primeTicks(User, Charging, 0.0F) : Current == null ? 0.0F : Current.getGameTime() - Charging.Start();
            int Stage = 0;
            for (int Index = 0; Index < StageTicks.length; Index++) {
                if (Ticks >= StageTicks[Index]) {
                    Stage = Index;
                }
            }
            return 0.25F + 0.1F * Stage;
        }
        if (Holder instanceof Player User && User.getCooldowns().getCooldownPercent(Stack.getItem(), 0.0F) > HotShare) {
            return Hot;
        }
        return GunItem.getAmmo(Stack) > 0 ? Idle : Empty;
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(UltralaserRegistry.MoteTeal.get(), UltralaserMoteParticle.TealProvider::new);
        Event.registerSpriteSet(UltralaserRegistry.MotePink.get(), UltralaserMoteParticle.PinkProvider::new);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(GunsClientEvents.AmmoLayer, PanelLayer, UltralaserHud::renderPanel);
        Event.registerAbove(GunsClientEvents.CrosshairLayer, ReticleLayer, UltralaserHud::renderReticle);
    }
}
