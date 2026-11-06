package net.rai.scp_unleashed.scp3008.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp3008ClientEvents {
    public static final float DayFogFar = 176.0f;
    public static final float DayFogNear = 0.1f;
    public static final float NightFogFar = 34.0f;
    public static final float ColourEase = 0.04f;
    private static Vec3 FogColour;

    private Scp3008ClientEvents() {
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(Scp3008Registry.location("scp3008_store"), new Scp3008StoreEffects());
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp3008Overlay.NightLayer, Scp3008Overlay::night);
        Event.registerAboveAll(Scp3008Overlay.FadeLayer, Scp3008Overlay::fade);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp3008ClientState.tick(Client);
        Scp3008Muzak.tick(Client);
    }

    private static boolean clearView(Minecraft Client) {
        return Scp3008ClientState.inside(Client) && Client.player != null && Client.gameRenderer.getMainCamera().getFluidInCamera() == FogType.NONE
                && !Client.player.hasEffect(MobEffects.BLINDNESS) && !Client.player.hasEffect(MobEffects.DARKNESS);
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!clearView(Client)) {
            return;
        }
        float Lit = Scp3008ClientState.brightness((float) Event.getPartialTick());
        float DayFar = Math.min(Event.getFarPlaneDistance(), DayFogFar);
        float Far = Mth.lerp(Lit, NightFogFar, DayFar);
        float Near = Mth.lerp(Lit, 0.0f, Far * DayFogNear);
        Event.setNearPlaneDistance(Near);
        Event.setFarPlaneDistance(Far);
        Event.setFogShape(FogShape.SPHERE);
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!clearView(Client) || Client.level == null) {
            FogColour = null;
            return;
        }
        Vec3 Eye = Event.getCamera().getPosition();
        BlockPos Spot = BlockPos.containing(Eye.x, Scp3008Plan.Stand, Eye.z);
        Vec3 Target = Vec3.fromRGB24(Client.level.getBiome(Spot).value().getFogColor());
        FogColour = FogColour == null ? Target : FogColour.lerp(Target, ColourEase);
        float Lit = Scp3008ClientState.brightness((float) Event.getPartialTick());
        double Scale = Mth.lerp(Lit, Scp3008StoreEffects.NightFog, 1.0f);
        Event.setRed((float) (FogColour.x * Scale));
        Event.setGreen((float) (FogColour.y * Scale));
        Event.setBlue((float) (FogColour.z * Scale));
    }

    @SubscribeEvent
    public static void selectMusic(SelectMusicEvent Event) {
        if (Scp3008ClientState.inside(Minecraft.getInstance())) {
            Event.setMusic(null);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp3008ClientState.reset();
        Scp3008Muzak.stop();
    }
}
